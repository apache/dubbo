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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.apache.dubbo.config.spring.util.AnnotationUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.config.spring.beans.factory.annotation.ServiceBeanNameBuilder.create;
import static org.apache.dubbo.config.spring.util.AnnotationUtils.getAttribute;
import static org.springframework.util.StringUtils.hasText;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that Consumer service {@link Reference} annotated fields
 *
 * @since 2.5.7
 */
public class ReferenceAnnotationBeanPostProcessor extends AnnotationInjectedBeanPostProcessor implements
        ApplicationContextAware, ApplicationListener {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    private final ConcurrentMap<String, ReferenceBean<?>> referenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private final ConcurrentHashMap<String, ReferenceBeanInvocationHandler> localReferenceBeanInvocationHandlerCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedFieldReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedMethodReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private ApplicationContext applicationContext;

    /**
     * To support the legacy annotation that is @com.alibaba.dubbo.config.annotation.Reference since 2.7.3
     */
    public ReferenceAnnotationBeanPostProcessor() {
        super(Reference.class, com.alibaba.dubbo.config.annotation.Reference.class);
    }

    /**
     * Gets all beans of {@link ReferenceBean}
     *
     * @return non-null read-only {@link Collection}
     * @since 2.5.9
     */
    public Collection<ReferenceBean<?>> getReferenceBeans() {
        return referenceBeanCache.values();
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
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        String referencedBeanName = buildReferencedBeanName(attributes, injectedType);

        ReferenceBean referenceBean = buildReferenceBeanIfAbsent(referencedBeanName, attributes, injectedType);

        registerReferenceBean(referencedBeanName, referenceBean, attributes, injectedType);

        cacheInjectedReferenceBean(referenceBean, injectedElement);

        return buildProxy(referencedBeanName, referenceBean, injectedType);
    }

    /**
     * Register an instance of {@link ReferenceBean} as a Spring Bean
     *
     * @param referencedBeanName the referenced bean name
     * @param referenceBean      the instance of {@link ReferenceBean}
     * @param attributes         the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass     the {@link Class class} of Service interface
     * @since 2.7.3
     */
    private void registerReferenceBean(String referencedBeanName, ReferenceBean referenceBean,
                                       AnnotationAttributes attributes,
                                       Class<?> interfaceClass) {

        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        String beanName = getReferenceBeanName(attributes, interfaceClass);

        if (beanFactory.containsBean(referencedBeanName)) { // If @Service bean is local one
            /**
             * Get  the @Service's BeanDefinition from {@link BeanFactory}
             * Refer to {@link ServiceAnnotationBeanPostProcessor#buildServiceBeanDefinition}
             */
            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(referencedBeanName);
            RuntimeBeanReference runtimeBeanReference = (RuntimeBeanReference) beanDefinition.getPropertyValues().get("ref");
            // The name of bean annotated @Service
            String serviceBeanName = runtimeBeanReference.getBeanName();
            // register Alias rather than a new bean name, in order to reduce duplicated beans
            beanFactory.registerAlias(serviceBeanName, beanName);
        } else { // Remote @Service Bean
            if (!beanFactory.containsBean(beanName)) {
                beanFactory.registerSingleton(beanName, referenceBean);
            }
        }
    }

    /**
     * Get the bean name of {@link ReferenceBean} if {@link Reference#id() id attribute} is present,
     * or {@link #generateReferenceBeanName(AnnotationAttributes, Class) generate}.
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return non-null
     * @since 2.7.3
     */
    private String getReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        // id attribute appears since 2.7.3
        String beanName = getAttribute(attributes, "id");
        if (!hasText(beanName)) {
            beanName = generateReferenceBeanName(attributes, interfaceClass);
        }
        return beanName;
    }

    /**
     * Build the bean name of {@link ReferenceBean}
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return
     * @since 2.7.3
     */
    private String generateReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        StringBuilder beanNameBuilder = new StringBuilder("@Reference");

        if (!attributes.isEmpty()) {
            beanNameBuilder.append('(');
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                beanNameBuilder.append(entry.getKey())
                        .append('=')
                        .append(entry.getValue())
                        .append(',');
            }
            // replace the latest "," to be ")"
            beanNameBuilder.setCharAt(beanNameBuilder.lastIndexOf(","), ')');
        }

        beanNameBuilder.append(" ").append(interfaceClass.getName());

        return beanNameBuilder.toString();
    }

    private Object buildProxy(String referencedBeanName, ReferenceBean referenceBean, Class<?> injectedType) {
        InvocationHandler handler = buildInvocationHandler(referencedBeanName, referenceBean);
        return Proxy.newProxyInstance(getClassLoader(), new Class[]{injectedType}, handler);
    }

    private InvocationHandler buildInvocationHandler(String referencedBeanName, ReferenceBean referenceBean) {

        ReferenceBeanInvocationHandler handler = localReferenceBeanInvocationHandlerCache.get(referencedBeanName);

        if (handler == null) {
            handler = new ReferenceBeanInvocationHandler(referenceBean);
        }

        if (applicationContext.containsBean(referencedBeanName)) { // Is local @Service Bean or not ?
            // ReferenceBeanInvocationHandler's initialization has to wait for current local @Service Bean has been exported.
            localReferenceBeanInvocationHandlerCache.put(referencedBeanName, handler);
        } else {
            // Remote Reference Bean should initialize immediately
            handler.init();
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
            Object result;
            try {
                if (bean == null) { // If the bean is not initialized, invoke init()
                    // issue: https://github.com/apache/dubbo/issues/3429
                    init();
                }
                result = method.invoke(bean, args);
            } catch (InvocationTargetException e) {
                // re-throws the actual Exception.
                throw e.getTargetException();
            }
            return result;
        }

        private void init() {
            this.bean = referenceBean.get();
        }
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {

        return buildReferencedBeanName(attributes, injectedType) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + AnnotationUtils.resolvePlaceholders(attributes, getEnvironment());
    }

    private String buildReferencedBeanName(AnnotationAttributes attributes, Class<?> injectedType) {

        ServiceBeanNameBuilder serviceBeanNameBuilder = create(attributes, injectedType, getEnvironment());

        return serviceBeanNameBuilder.build();
    }

    private ReferenceBean buildReferenceBeanIfAbsent(String referencedBeanName, AnnotationAttributes attributes,
                                                     Class<?> referencedType)
            throws Exception {

        ReferenceBean<?> referenceBean = referenceBeanCache.get(referencedBeanName);

        if (referenceBean == null) {
            ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder
                    .create(attributes, applicationContext)
                    .interfaceClass(referencedType);
            referenceBean = beanBuilder.build();
            referenceBeanCache.put(referencedBeanName, referenceBean);
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
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ServiceBeanExportedEvent) {
            onServiceBeanExportEvent((ServiceBeanExportedEvent) event);
        } else if (event instanceof ContextRefreshedEvent) {
            onContextRefreshedEvent((ContextRefreshedEvent) event);
        }
    }

    private void onServiceBeanExportEvent(ServiceBeanExportedEvent event) {
        ServiceBean serviceBean = event.getServiceBean();
        initReferenceBeanInvocationHandler(serviceBean);
    }

    private void initReferenceBeanInvocationHandler(ServiceBean serviceBean) {
        String serviceBeanName = serviceBean.getBeanName();
        // Remove ServiceBean when it's exported
        ReferenceBeanInvocationHandler handler = localReferenceBeanInvocationHandlerCache.remove(serviceBeanName);
        // Initialize
        if (handler != null) {
            handler.init();
        }
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {

    }


    @Override
    public void destroy() throws Exception {
        super.destroy();
        this.referenceBeanCache.clear();
        this.localReferenceBeanInvocationHandlerCache.clear();
        this.injectedFieldReferenceBeanCache.clear();
        this.injectedMethodReferenceBeanCache.clear();
    }
}
