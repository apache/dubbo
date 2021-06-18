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
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.spring.util.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;

/**
 * Abstract common {@link BeanPostProcessor} implementation for customized annotation that annotated injected-object.
 */
@SuppressWarnings("unchecked")
public abstract class AbstractAnnotationBeanPostProcessor extends
        InstantiationAwareBeanPostProcessorAdapter implements MergedBeanDefinitionPostProcessor, Ordered,
        BeanFactoryAware, BeanClassLoaderAware, EnvironmentAware, DisposableBean {

    private final static int CACHE_SIZE = Integer.getInteger("", 32);

    private final Log logger = LogFactory.getLog(getClass());

    private final Class<? extends Annotation>[] annotationTypes;

    private final ConcurrentMap<String, AbstractAnnotationBeanPostProcessor.AnnotatedInjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<String, AbstractAnnotationBeanPostProcessor.AnnotatedInjectionMetadata>(CACHE_SIZE);

    private final ConcurrentMap<String, Object> injectedObjectsCache = new ConcurrentHashMap<String, Object>(CACHE_SIZE);

    private ConfigurableListableBeanFactory beanFactory;

    private Environment environment;

    private ClassLoader classLoader;

    private int order = Ordered.LOWEST_PRECEDENCE;

    /**
     * @param annotationTypes the multiple types of {@link Annotation annotations}
     */
    public AbstractAnnotationBeanPostProcessor(Class<? extends Annotation>... annotationTypes) {
        Assert.notEmpty(annotationTypes, "The argument of annotations' types must not empty");
        this.annotationTypes = annotationTypes;
    }

    private static <T> Collection<T> combine(Collection<? extends T>... elements) {
        List<T> allElements = new ArrayList<T>();
        for (Collection<? extends T> e : elements) {
            allElements.addAll(e);
        }
        return allElements;
    }

    /**
     * Annotation type
     *
     * @return non-null
     * @deprecated 2.7.3, uses {@link #getAnnotationTypes()}
     */
    @Deprecated
    public final Class<? extends Annotation> getAnnotationType() {
        return annotationTypes[0];
    }

    protected final Class<? extends Annotation>[] getAnnotationTypes() {
        return annotationTypes;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
                "AnnotationInjectedBeanPostProcessor requires a ConfigurableListableBeanFactory");
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType != null) {
            AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
            metadata.checkConfigMembers(beanDefinition);
            try {
                prepareInjection(metadata);
            } catch (Exception e) {
                logger.error("Prepare injection of @"+getAnnotationType().getSimpleName()+" failed", e);
            }
        }
    }

    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

        try {
            AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
            prepareInjection(metadata);
            metadata.inject(bean, beanName, pvs);
        } catch (BeansException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of @" + getAnnotationType().getSimpleName()
                    + " dependencies is failed", ex);
        }
        return pvs;
    }


    /**
     * Finds {@link InjectionMetadata.InjectedElement} Metadata from annotated fields
     *
     * @param beanClass The {@link Class} of Bean
     * @return non-null {@link List}
     */
    private List<AbstractAnnotationBeanPostProcessor.AnnotatedFieldElement> findFieldAnnotationMetadata(final Class<?> beanClass) {

        final List<AbstractAnnotationBeanPostProcessor.AnnotatedFieldElement> elements = new LinkedList<AbstractAnnotationBeanPostProcessor.AnnotatedFieldElement>();

        ReflectionUtils.doWithFields(beanClass, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

                for (Class<? extends Annotation> annotationType : getAnnotationTypes()) {

                    AnnotationAttributes attributes = getAnnotationAttributes(field, annotationType, getEnvironment(), true, true);

                    if (attributes != null) {

                        if (Modifier.isStatic(field.getModifiers())) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("@" + annotationType.getName() + " is not supported on static fields: " + field);
                            }
                            return;
                        }

                        elements.add(new AnnotatedFieldElement(field, attributes));
                    }
                }
            }
        });

        return elements;

    }

    /**
     * Finds {@link InjectionMetadata.InjectedElement} Metadata from annotated methods
     *
     * @param beanClass The {@link Class} of Bean
     * @return non-null {@link List}
     */
    private List<AbstractAnnotationBeanPostProcessor.AnnotatedMethodElement> findAnnotatedMethodMetadata(final Class<?> beanClass) {

        final List<AbstractAnnotationBeanPostProcessor.AnnotatedMethodElement> elements = new LinkedList<AbstractAnnotationBeanPostProcessor.AnnotatedMethodElement>();

        ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

                Method bridgedMethod = findBridgedMethod(method);

                if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                    return;
                }

                if (method.getAnnotation(Bean.class) != null) {
                    // DO NOT inject to Java-config class's @Bean method
                    return;
                }

                for (Class<? extends Annotation> annotationType : getAnnotationTypes()) {

                    AnnotationAttributes attributes = getAnnotationAttributes(bridgedMethod, annotationType, getEnvironment(), true, true);

                    if (attributes != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalStateException("When using @"+annotationType.getName() +" to inject interface proxy, it is not supported on static methods: "+method);
                        }
                        if (method.getParameterTypes().length != 1) {
                            throw new IllegalStateException("When using @"+annotationType.getName() +" to inject interface proxy, the method must have only one parameter: "+method);
                        }
                        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
                        elements.add(new AnnotatedMethodElement(method, pd, attributes));
                    }
                }
            }
        });

        return elements;
    }

    private AbstractAnnotationBeanPostProcessor.AnnotatedInjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass) {
        Collection<AbstractAnnotationBeanPostProcessor.AnnotatedFieldElement> fieldElements = findFieldAnnotationMetadata(beanClass);
        Collection<AbstractAnnotationBeanPostProcessor.AnnotatedMethodElement> methodElements = findAnnotatedMethodMetadata(beanClass);
        return new AnnotatedInjectionMetadata(beanClass, fieldElements, methodElements);
    }

    protected AnnotatedInjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        AbstractAnnotationBeanPostProcessor.AnnotatedInjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (needsRefreshInjectionMetadata(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);

                if (needsRefreshInjectionMetadata(metadata, clazz)) {
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

    // Use custom check method to compatible with Spring 4.x
    private boolean needsRefreshInjectionMetadata(AnnotatedInjectionMetadata metadata, Class<?> clazz) {
        return (metadata == null || metadata.needsRefresh(clazz));
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
     * Get injected-object from specified {@link AnnotationAttributes annotation attributes} and Bean Class
     *
     * @param attributes      {@link AnnotationAttributes the annotation attributes}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link AnnotatedInjectElement}
     * @return An injected object
     * @throws Exception If getting is failed
     */
    protected Object getInjectedObject(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       AnnotatedInjectElement injectedElement) throws Exception {

//        String cacheKey = buildInjectedObjectCacheKey(attributes, bean, beanName, injectedType, injectedElement);
//
//        Object injectedObject = injectedObjectsCache.get(cacheKey);
//
//        if (injectedObject == null) {
//            injectedObject = doGetInjectedBean(attributes, bean, beanName, injectedType, injectedElement);
//            // Customized inject-object if necessary
//            injectedObjectsCache.put(cacheKey, injectedObject);
//        }
//        return injectedObject;

        return doGetInjectedBean(attributes, bean, beanName, injectedType, injectedElement);
    }

    /**
     * Prepare injection data after found injection elements
     * @param metadata
     * @throws Exception
     */
    protected void prepareInjection(AnnotatedInjectionMetadata metadata) throws Exception {
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
     * @param attributes      {@link AnnotationAttributes the annotation attributes}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link AnnotatedInjectElement}
     * @return The injected object
     * @throws Exception If resolving an injected object is failed.
     */
    protected abstract Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                                AnnotatedInjectElement injectedElement) throws Exception;

    /**
     * Build a cache key for injected-object. The context objects could help this method if
     * necessary :
     * <ul>
     * <li>{@link #getBeanFactory() BeanFactory}</li>
     * <li>{@link #getClassLoader() ClassLoader}</li>
     * <li>{@link #getEnvironment() Environment}</li>
     * </ul>
     *
     * @param attributes      {@link AnnotationAttributes the annotation attributes}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link AnnotatedInjectElement}
     * @return Bean cache key
     */
//    protected abstract String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
//                                                          Class<?> injectedType,
//                                                          AnnotatedInjectElement injectedElement);

    /**
     * {@link Annotation Annotated} {@link InjectionMetadata} implementation
     */
    protected static class AnnotatedInjectionMetadata extends InjectionMetadata {

        private Class<?> targetClass;
        private final Collection<AbstractAnnotationBeanPostProcessor.AnnotatedFieldElement> fieldElements;

        private final Collection<AbstractAnnotationBeanPostProcessor.AnnotatedMethodElement> methodElements;

        public AnnotatedInjectionMetadata(Class<?> targetClass, Collection<AbstractAnnotationBeanPostProcessor.AnnotatedFieldElement> fieldElements,
                                          Collection<AbstractAnnotationBeanPostProcessor.AnnotatedMethodElement> methodElements) {
            super(targetClass, combine(fieldElements, methodElements));
            this.targetClass = targetClass;
            this.fieldElements = fieldElements;
            this.methodElements = methodElements;
        }

        public Collection<AbstractAnnotationBeanPostProcessor.AnnotatedFieldElement> getFieldElements() {
            return fieldElements;
        }

        public Collection<AbstractAnnotationBeanPostProcessor.AnnotatedMethodElement> getMethodElements() {
            return methodElements;
        }

        //@Override // since Spring 5.2.4
        protected boolean needsRefresh(Class<?> clazz) {
            if (this.targetClass == clazz) {
                return false;
            }
            //IGNORE Spring CGLIB enhanced class
            if (targetClass.isAssignableFrom(clazz) &&  clazz.getName().contains("$$EnhancerBySpringCGLIB$$")) {
                return false;
            }
            return true;
        }
    }

    /**
     * {@link Annotation Annotated} {@link Method} {@link InjectionMetadata.InjectedElement}
     */
    protected class AnnotatedInjectElement extends InjectionMetadata.InjectedElement {

        protected final AnnotationAttributes attributes;

        protected volatile Object injectedObject;

        private Class<?> injectedType;

        protected AnnotatedInjectElement(Member member, PropertyDescriptor pd, AnnotationAttributes attributes) {
            super(member, pd);
            this.attributes = attributes;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Object injectedObject = getInjectedObject(attributes, bean, beanName, getInjectedType(), this);

            if (member instanceof Field) {
                Field field = (Field) member;
                ReflectionUtils.makeAccessible(field);
                field.set(bean, injectedObject);
            } else if (member instanceof Method) {
                Method method = (Method) member;
                ReflectionUtils.makeAccessible(method);
                method.invoke(bean, injectedObject);
            }
        }

        public Class<?> getInjectedType() throws ClassNotFoundException {
            if (injectedType == null) {
                if (this.isField) {
                    injectedType = ((Field) this.member).getType();
                }
                else if (this.pd != null) {
                    return this.pd.getPropertyType();
                }
                else {
                    Method method = (Method) this.member;
                    if (method.getParameterTypes().length > 0) {
                        injectedType = method.getParameterTypes()[0];
                    } else {
                        throw new IllegalStateException("get injected type failed");
                    }
                }
            }
            return injectedType;
        }

        public String getPropertyName() {
            if (member instanceof Field) {
                Field field = (Field) member;
                return field.getName();
            } else if (this.pd != null) {
                // If it is method element, using propertyName of PropertyDescriptor
                return pd.getName();
            } else {
                Method method = (Method) this.member;
                return method.getName();
            }
        }
    }

    protected class AnnotatedMethodElement extends AnnotatedInjectElement {

        protected final Method method;

        protected AnnotatedMethodElement(Method method, PropertyDescriptor pd, AnnotationAttributes attributes) {
            super(method, pd, attributes);
            this.method = method;
        }
    }

    public class AnnotatedFieldElement extends AnnotatedInjectElement {

        protected final Field field;

        protected AnnotatedFieldElement(Field field, AnnotationAttributes attributes) {
            super(field, null, attributes);
            this.field = field;
        }
    }
}
