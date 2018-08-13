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
import java.util.*;
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

    private ApplicationContext applicationContext;

    private final List<ReferenceBean<?>> referenceBeans = new LinkedList<ReferenceBean<?>>();

    private final List<ReferenceBeanInvocationHandler> referenceBeanInvocationHandlers =
            new LinkedList<ReferenceBeanInvocationHandler>();

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedFieldReferenceBeanMap =
            new ConcurrentHashMap<InjectionMetadata.InjectedElement, ReferenceBean<?>>();

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedMethodReferenceBeanMap =
            new ConcurrentHashMap<InjectionMetadata.InjectedElement, ReferenceBean<?>>();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Gets all beans of {@link ReferenceBean}
     *
     * @return non-null {@link Collection}
     * @since 2.5.9
     */
    public Collection<ReferenceBean<?>> getReferenceBeans() {
        return Collections.unmodifiableCollection(referenceBeans);
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
        return Collections.unmodifiableMap(injectedFieldReferenceBeanMap);
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected method.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedMethodReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedMethodReferenceBeanMap);
    }

    @Override
    protected Object doGetInjectedBean(Reference reference, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        ReferenceBean referenceBean = buildReferenceBean(reference, injectedType, getClassLoader(), injectedElement);

        InvocationHandler handler = buildReferenceBeanInvocationHandler(referenceBean);

        Object proxy = Proxy.newProxyInstance(getClassLoader(), new Class[]{injectedType}, handler);

        return proxy;
    }

    private InvocationHandler buildReferenceBeanInvocationHandler(ReferenceBean referenceBean) {
        ReferenceBeanInvocationHandler handler = new ReferenceBeanInvocationHandler(referenceBean);
        referenceBeanInvocationHandlers.add(handler);
        return handler;
    }

    private static class ReferenceBeanInvocationHandler implements InvocationHandler {

        private final ReferenceBean referenceBean;

        private ReferenceBeanInvocationHandler(ReferenceBean referenceBean) {
            this.referenceBean = referenceBean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object bean = referenceBean.get();
            return method.invoke(bean, args);
        }
    }

    @Override
    protected String buildInjectedObjectCacheKey(Reference reference, Object bean, String beanName, Class<?> injectedType) {

        String interfaceName = resolveInterfaceName(reference, injectedType);

        String key = reference.url() + "/" + interfaceName +
                "/" + reference.version() +
                "/" + reference.group();

        key = getEnvironment().resolvePlaceholders(key);

        return key;
    }

    private ReferenceBean buildReferenceBean(Reference reference, Class<?> referencedType,
                                             ClassLoader classLoader, InjectionMetadata.InjectedElement injectedElement) throws Exception {

        ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder
                .create(reference, classLoader, applicationContext)
                .interfaceClass(referencedType);

        ReferenceBean<?> referenceBean = beanBuilder.build();

        referenceBeans.add(referenceBean);

        if (injectedElement.getMember() instanceof Field) {
            injectedFieldReferenceBeanMap.put(injectedElement, referenceBean);
        } else if (injectedElement.getMember() instanceof Method) {
            injectedMethodReferenceBeanMap.put(injectedElement, referenceBean);
        }

        return referenceBean;

    }


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (referenceBeanInvocationHandlers.isEmpty()) {
            return;
        }

        // clear all
        referenceBeanInvocationHandlers.clear();
    }
}
