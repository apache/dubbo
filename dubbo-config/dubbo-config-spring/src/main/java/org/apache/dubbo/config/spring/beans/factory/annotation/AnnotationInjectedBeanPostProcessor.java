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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.config.spring.util.ClassUtils.resolveGenericType;
import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedAnnotation;

/**
 * Abstract generic {@link BeanPostProcessor} implementation for customized annotation that annotated injected-object.
 *
 * The source code is cloned from https://github.com/alibaba/spring-context-support/blob/1.0.2/src/main/java/com/alibaba/spring/beans/factory/annotation/AnnotationInjectedBeanPostProcessor.java
 *
 * @since 2.6.6
 */
public abstract class AnnotationInjectedBeanPostProcessor<A extends Annotation> extends
        InstantiationAwareBeanPostProcessorAdapter implements MergedBeanDefinitionPostProcessor, PriorityOrdered,
        BeanFactoryAware, BeanClassLoaderAware, EnvironmentAware, DisposableBean {

    private final static int CACHE_SIZE = Integer.getInteger("", 32);

    private final Log logger = LogFactory.getLog(getClass());

    private final Class<A> annotationType;

    private final ConcurrentMap<String, AnnotationInjectedBeanPostProcessor.AnnotatedInjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<String, AnnotationInjectedBeanPostProcessor.AnnotatedInjectionMetadata>(CACHE_SIZE);

    private final ConcurrentMap<String, Object> injectedObjectsCache = new ConcurrentHashMap<>(CACHE_SIZE);

    private ConfigurableListableBeanFactory beanFactory;

    private Environment environment;

    private ClassLoader classLoader;

    private int order = Ordered.LOWEST_PRECEDENCE;

    public AnnotationInjectedBeanPostProcessor() {
        this.annotationType = resolveGenericType(getClass());
    }

    @SafeVarargs
    private static <T> Collection<T> combine(Collection<? extends T>... elements) {
        List<T> allElements = new ArrayList<>();
        for (Collection<? extends T> e : elements) {
            allElements.addAll(e);
        }
        return allElements;
    }

    /**
     * Annotation type
     *
     * @return non-null
     */
    public final Class<A> getAnnotationType() {
        return annotationType;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
                "AnnotationInjectedBeanPostProcessor requires a ConfigurableListableBeanFactory");
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeanCreationException {

        InjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of @" + getAnnotationType().getName()
                    + " dependencies is failed", ex);
        }
        return pvs;
    }


    /**
     * Finds {@link InjectionMetadata.InjectedElement} Metadata from annotated {@link A} fields
     *
     * @param beanClass The {@link Class} of Bean
     * @return non-null {@link List}
     */
    private List<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement> findFieldAnnotationMetadata(final Class<?> beanClass) {

        final List<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement> elements = new LinkedList<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement>();

        ReflectionUtils.doWithFields(beanClass, field -> {

            A annotation = getMergedAnnotation(field, getAnnotationType());

            if (annotation != null) {

                if (Modifier.isStatic(field.getModifiers())) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("@" + getAnnotationType().getName() + " is not supported on static fields: " + field);
                    }
                    return;
                }

                elements.add(new AnnotatedFieldElement(field, annotation));
            }

        });

        return elements;

    }

    /**
     * Finds {@link InjectionMetadata.InjectedElement} Metadata from annotated {@link A @A} methods
     *
     * @param beanClass The {@link Class} of Bean
     * @return non-null {@link List}
     */
    private List<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement> findAnnotatedMethodMetadata(final Class<?> beanClass) {

        final List<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement> elements = new LinkedList<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement>();

        ReflectionUtils.doWithMethods(beanClass, method -> {

            Method bridgedMethod = findBridgedMethod(method);

            if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                return;
            }

            A annotation = findMergedAnnotation(bridgedMethod, getAnnotationType());

            if (annotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("@" + getAnnotationType().getSimpleName() + " annotation is not supported on static methods: " + method);
                    }
                    return;
                }
                if (method.getParameterTypes().length == 0) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("@" + getAnnotationType().getSimpleName() + " annotation should only be used on methods with parameters: " +
                                method);
                    }
                }
                PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
                elements.add(new AnnotatedMethodElement(method, pd, annotation));
            }
        });

        return elements;

    }


    private AnnotationInjectedBeanPostProcessor.AnnotatedInjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass) {
        Collection<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement> fieldElements = findFieldAnnotationMetadata(beanClass);
        Collection<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement> methodElements = findAnnotatedMethodMetadata(beanClass);
        return new AnnotationInjectedBeanPostProcessor.AnnotatedInjectionMetadata(beanClass, fieldElements, methodElements);

    }

    private InjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        AnnotationInjectedBeanPostProcessor.AnnotatedInjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    try {
                        metadata = buildAnnotatedMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to introspect object class [" + clazz.getName() +
                                "] for annotation metadata: could not find class that it depends on", err);
                    }
                }
            }
        }
        return metadata;
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType != null) {
            InjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
            metadata.checkConfigMembers(beanDefinition);
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public void destroy() throws Exception {

        for (Object object : injectedObjectsCache.values()) {
            if (logger.isInfoEnabled()) {
                logger.info(object + " was destroying!");
            }

            if (object instanceof DisposableBean) {
                ((DisposableBean) object).destroy();
            }
        }

        injectionMetadataCache.clear();
        injectedObjectsCache.clear();

        if (logger.isInfoEnabled()) {
            logger.info(getClass() + " was destroying!");
        }

    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    protected Environment getEnvironment() {
        return environment;
    }

    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    protected ConfigurableListableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * Gets all injected-objects.
     *
     * @return non-null {@link Collection}
     */
    protected Collection<Object> getInjectedObjects() {
        return this.injectedObjectsCache.values();
    }

    /**
     * Get injected-object from specified {@link A annotation} and Bean Class
     *
     * @param annotation      {@link A annotation}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link InjectionMetadata.InjectedElement}
     * @return An injected object
     * @throws Exception If getting is failed
     */
    protected Object getInjectedObject(A annotation, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        String cacheKey = buildInjectedObjectCacheKey(annotation, bean, beanName, injectedType, injectedElement);

        Object injectedObject = injectedObjectsCache.get(cacheKey);

        if (injectedObject == null) {
            injectedObject = doGetInjectedBean(annotation, bean, beanName, injectedType, injectedElement);
            // Customized inject-object if necessary
            injectedObjectsCache.putIfAbsent(cacheKey, injectedObject);
        }

        return injectedObject;

    }

    /**
     * Subclass must implement this method to get injected-object. The context objects could help this method if
     * necessary :
     * <ul>
     * <li>{@link #getBeanFactory() BeanFactory}</li>
     * <li>{@link #getClassLoader() ClassLoader}</li>
     * <li>{@link #getEnvironment() Environment}</li>
     * </ul>
     *
     * @param annotation      {@link A annotation}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link InjectionMetadata.InjectedElement}
     * @return The injected object
     * @throws Exception If resolving an injected object is failed.
     */
    protected abstract Object doGetInjectedBean(A annotation, Object bean, String beanName, Class<?> injectedType,
                                                InjectionMetadata.InjectedElement injectedElement) throws Exception;

    /**
     * Build a cache key for injected-object. The context objects could help this method if
     * necessary :
     * <ul>
     * <li>{@link #getBeanFactory() BeanFactory}</li>
     * <li>{@link #getClassLoader() ClassLoader}</li>
     * <li>{@link #getEnvironment() Environment}</li>
     * </ul>
     *
     * @param annotation      {@link A annotation}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link InjectionMetadata.InjectedElement}
     * @return Bean cache key
     */
    protected abstract String buildInjectedObjectCacheKey(A annotation, Object bean, String beanName,
                                                          Class<?> injectedType,
                                                          InjectionMetadata.InjectedElement injectedElement);

    /**
     * Get {@link Map} in injected field.
     *
     * @return non-null ready-only {@link Map}
     */
    protected Map<InjectionMetadata.InjectedElement, Object> getInjectedFieldObjectsMap() {

        Map<InjectionMetadata.InjectedElement, Object> injectedElementBeanMap =
                new LinkedHashMap<InjectionMetadata.InjectedElement, Object>();

        for (AnnotationInjectedBeanPostProcessor.AnnotatedInjectionMetadata metadata : injectionMetadataCache.values()) {

            Collection<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement> fieldElements = metadata.getFieldElements();

            for (AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement fieldElement : fieldElements) {

                injectedElementBeanMap.put(fieldElement, fieldElement.bean);

            }

        }

        return Collections.unmodifiableMap(injectedElementBeanMap);

    }

    /**
     * Get {@link Map} in injected method.
     *
     * @return non-null {@link Map}
     */
    protected Map<InjectionMetadata.InjectedElement, Object> getInjectedMethodObjectsMap() {

        Map<InjectionMetadata.InjectedElement, Object> injectedElementBeanMap =
                new LinkedHashMap<InjectionMetadata.InjectedElement, Object>();

        for (AnnotationInjectedBeanPostProcessor.AnnotatedInjectionMetadata metadata : injectionMetadataCache.values()) {

            Collection<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement> methodElements = metadata.getMethodElements();

            for (AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement methodElement : methodElements) {

                injectedElementBeanMap.put(methodElement, methodElement.object);

            }

        }

        return Collections.unmodifiableMap(injectedElementBeanMap);

    }

    /**
     * {@link A} {@link InjectionMetadata} implementation
     */
    private class AnnotatedInjectionMetadata extends InjectionMetadata {

        private final Collection<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement> fieldElements;

        private final Collection<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement> methodElements;

        public AnnotatedInjectionMetadata(Class<?> targetClass, Collection<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement> fieldElements,
                                          Collection<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement> methodElements) {
            super(targetClass, combine(fieldElements, methodElements));
            this.fieldElements = fieldElements;
            this.methodElements = methodElements;
        }

        public Collection<AnnotationInjectedBeanPostProcessor.AnnotatedFieldElement> getFieldElements() {
            return fieldElements;
        }

        public Collection<AnnotationInjectedBeanPostProcessor.AnnotatedMethodElement> getMethodElements() {
            return methodElements;
        }
    }

    /**
     * {@link A} {@link Method} {@link InjectionMetadata.InjectedElement}
     */
    private class AnnotatedMethodElement extends InjectionMetadata.InjectedElement {

        private final Method method;

        private final A annotation;

        private volatile Object object;

        protected AnnotatedMethodElement(Method method, PropertyDescriptor pd, A annotation) {
            super(method, pd);
            this.method = method;
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = pd.getPropertyType();

            Object injectedObject = getInjectedObject(annotation, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(method);

            method.invoke(bean, injectedObject);

        }

    }

    /**
     * {@link A} {@link Field} {@link InjectionMetadata.InjectedElement}
     */
    public class AnnotatedFieldElement extends InjectionMetadata.InjectedElement {

        private final Field field;

        private final A annotation;

        private volatile Object bean;

        protected AnnotatedFieldElement(Field field, A annotation) {
            super(field, null);
            this.field = field;
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = field.getType();

            Object injectedObject = getInjectedObject(annotation, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(field);

            field.set(bean, injectedObject);

        }

    }
}
