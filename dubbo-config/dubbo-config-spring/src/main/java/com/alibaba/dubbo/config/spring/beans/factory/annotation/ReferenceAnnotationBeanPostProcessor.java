/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.spring.beans.factory.annotation.CustomizedAnnotationBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that Consumer service {@link Reference} annotated fields
 *
 * @since 2.5.7
 */
public class ReferenceAnnotationBeanPostProcessor extends CustomizedAnnotationBeanPostProcessor<Reference>
        implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    private final ConcurrentMap<String, ReferenceBean<?>> referenceBeanCache =
            new ConcurrentHashMap<String, ReferenceBean<?>>(CACHE_SIZE);

    private final ConcurrentHashMap<String, Object> proxyCache = new ConcurrentHashMap<String, Object>(CACHE_SIZE);

    private final ConcurrentHashMap<String, ReferenceBeanInvocationHandler> referenceBeanInvocationHandlerCache =
            new ConcurrentHashMap<String, ReferenceBeanInvocationHandler>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedFieldReferenceBeanCache =
            new ConcurrentHashMap<InjectionMetadata.InjectedElement, ReferenceBean<?>>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedMethodReferenceBeanCache =
            new ConcurrentHashMap<InjectionMetadata.InjectedElement, ReferenceBean<?>>(CACHE_SIZE);

    private ApplicationContext applicationContext;

    /**
     * Gets all beans of {@link ReferenceBean}
     *
     * @return non-null read-only {@link Collection}
     * @since 2.5.9
     */
    public Collection<ReferenceBean<?>> getReferenceBeans() {
        return referenceBeanCache.values();
    }

    private static String resolveInterfaceName(Reference reference, Class<?> beanClass)
            throws IllegalStateException {

        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (beanClass.isInterface()) {
            interfaceName = beanClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Reference undefined interfaceClass or interfaceName, and the property type "
                            + beanClass.getName() + " is not a interface.");
        }

        return interfaceName;

    }


    /**
     * Get {@link ReferenceBean} {@link Map} in injected field.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedFieldReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedFieldReferenceBeanCache);
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected method.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedMethodReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedMethodReferenceBeanCache);
    }

    @Override
    protected Object doGetInjectedBean(Reference reference, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        String referenceBeanCacheKey = buildReferenceBeanCacheKey(reference, injectedType);

        ReferenceBean referenceBean = buildReferenceBeanIfAbsent(referenceBeanCacheKey, reference, injectedType, getClassLoader());

        cacheInjectedReferenceBean(referenceBean, injectedElement);

        Object proxy = buildProxyIfAbsent(referenceBeanCacheKey, referenceBean, injectedType);

        return proxy;
    }

    private Object buildProxyIfAbsent(String referenceBeanCacheKey, ReferenceBean referenceBean, Class<?> injectedType) {

        Object proxy = proxyCache.get(referenceBeanCacheKey);

        if (proxy == null) {
            InvocationHandler handler = buildInvocationHandlerIfAbsent(referenceBeanCacheKey, referenceBean);
            proxy = Proxy.newProxyInstance(getClassLoader(), new Class[]{injectedType}, handler);
            proxyCache.put(referenceBeanCacheKey, proxy);
        }

        return proxy;
    }

    private InvocationHandler buildInvocationHandlerIfAbsent(String referenceBeanCacheKey, ReferenceBean referenceBean) {

        ReferenceBeanInvocationHandler handler = referenceBeanInvocationHandlerCache.get(referenceBeanCacheKey);

        if (handler == null) {
            handler = new ReferenceBeanInvocationHandler(referenceBean);
            referenceBeanInvocationHandlerCache.put(referenceBeanCacheKey, handler);
        }

        return handler;
    }

    private static class ReferenceBeanInvocationHandler implements InvocationHandler {

        private final ReferenceBean referenceBean;

        private Object bean;

        private ReferenceBeanInvocationHandler(ReferenceBean referenceBean) {
            this.referenceBean = referenceBean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(bean, args);
        }

        private void init() {
            this.bean = referenceBean.get();
        }
    }

    @Override
    protected String buildInjectedObjectCacheKey(Reference reference, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {

        String key = buildReferenceBeanCacheKey(reference, injectedType) +
                "?source=" + (injectedElement.getMember());

        return key;
    }

    private String buildReferenceBeanCacheKey(Reference reference, Class<?> injectedType) {

        String interfaceName = resolveInterfaceName(reference, injectedType);

        String key = reference.url() +
                "/" + interfaceName +
                "/" + reference.version() +
                "/" + reference.group();

        return getEnvironment().resolvePlaceholders(key);
    }

    private ReferenceBean buildReferenceBeanIfAbsent(String referenceBeanCacheKey, Reference reference,
                                                     Class<?> referencedType, ClassLoader classLoader)
            throws Exception {

        ReferenceBean<?> referenceBean = referenceBeanCache.get(referenceBeanCacheKey);

        if (referenceBean == null) {
            ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder
                    .create(reference, classLoader, applicationContext)
                    .interfaceClass(referencedType);
            referenceBean = beanBuilder.build();
            referenceBeanCache.put(referenceBeanCacheKey, referenceBean);
        }

        return referenceBean;
    }

    private void cacheInjectedReferenceBean(ReferenceBean referenceBean,
                                            InjectionMetadata.InjectedElement injectedElement) {
        if (injectedElement.getMember() instanceof Field) {
            injectedFieldReferenceBeanCache.put(injectedElement, referenceBean);
        } else if (injectedElement.getMember() instanceof Method) {
            injectedMethodReferenceBeanCache.put(injectedElement, referenceBean);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (proxyCache.isEmpty() || referenceBeanCache.isEmpty() || referenceBeanInvocationHandlerCache.isEmpty()) {
            return;
        }

        // initialize all
        for (ReferenceBeanInvocationHandler handler : referenceBeanInvocationHandlerCache.values()) {
            handler.init();
        }

        // clear Proxies and Handlers
        this.proxyCache.clear();
        this.referenceBeanInvocationHandlerCache.clear();
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        this.proxyCache.clear();
        this.referenceBeanCache.clear();
        this.referenceBeanInvocationHandlerCache.clear();
        this.injectedFieldReferenceBeanCache.clear();
        this.injectedMethodReferenceBeanCache.clear();
    }
}
