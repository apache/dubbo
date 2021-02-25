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
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used for autowired {@link org.apache.dubbo.config.spring.ReferenceBean}
 * 1. Store all BeanDefinition contains {@link org.apache.dubbo.config.annotation.Reference} field or method.
 * 2. When one class has {@link Autowired} or {@link Resource} or {@link javax.inject.Inject} a reference field,
 * call getBean method store in the previous step first.
 * @see BeanFactory#getBean
 * @author anLA7856
 */
public class ReferenceBeanFactoryPostProcessor implements BeanFactoryPostProcessor, MergedBeanDefinitionPostProcessor, BeanFactoryAware {

    /**
     * The bean name of {@link ReferenceBeanFactoryPostProcessor}
     */
    public static final String BEAN_NAME = "referenceBeanFactoryPostProcessor";

    protected final Log logger = LogFactory.getLog(getClass());

    private final Set<Class<? extends Annotation>> referenceAnnotationTypes = new LinkedHashSet<>(4);
    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

    /**
     * store the bean names who has the ReferenceClass
     * key is the ReferenceClass, Set are bean names
     */
    private Map<String, Set<String>> referenceIdToBeanNames = new ConcurrentHashMap<>(256);
    private Map<String, Boolean> referenceHasInit = new ConcurrentHashMap<>(256);

    private BeanFactory beanFactory;

    public ReferenceBeanFactoryPostProcessor() {
        this.referenceAnnotationTypes.add(Reference.class);
        this.referenceAnnotationTypes.add(DubboReference.class);
        this.referenceAnnotationTypes.add(com.alibaba.dubbo.config.annotation.Reference.class);
        this.autowiredAnnotationTypes.add(Autowired.class);
        this.autowiredAnnotationTypes.add(Resource.class);
        try {
            this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
                    ClassUtils.forName("javax.inject.Inject", ReferenceBeanFactoryPostProcessor.class.getClassLoader()));
            logger.trace("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
        }
        catch (ClassNotFoundException ex) {
            // JSR-330 API not available - simply skip.
        }
    }

    private void buildReferenceMetadata(final Class<?> clazz, String beanName) {
        findOrInjectAnnotation(clazz, (FindCallback) this::cacheRef, beanName, referenceAnnotationTypes, Reference.class.getName());
    }

    private void findOrInjectAnnotation(Class<?> targetClass, CallBack callBack, String beanName, Set<Class<? extends Annotation>> annotationTypes, String type) {
        do {
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                AnnotationAttributes ann = findAnnotation(field, annotationTypes);
                if (ann != null) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        if (logger.isInfoEnabled()) {
                            logger.info(type + " is not supported on static fields: " + field);
                        }
                        return;
                    }
                    String name = field.getType().getName();
                    callBack.callback(name, beanName);
                }
            });

            Class<?> finalTargetClass = targetClass;
            ReflectionUtils.doWithLocalMethods(targetClass, method -> {
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
                if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                    return;
                }
                AnnotationAttributes ann = findAnnotation(bridgedMethod, annotationTypes);
                if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, finalTargetClass))) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        if (logger.isInfoEnabled()) {
                            logger.info(type + " annotation is not supported on static methods: " + method);
                        }
                        return;
                    }
                    if (method.getParameterCount() == 0) {
                        if (logger.isInfoEnabled()) {
                            logger.info(type + " annotation should only be used on methods with parameters: " +
                                    method);
                        }
                    }
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    for (Class<?> clazz : parameterTypes){
                        String name = clazz.getName();
                        callBack.callback(name, beanName);
                    }
                }
            });

            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);
    }

    private void cacheRef(String name, String beanName) {
        Set<String> beanNames = referenceIdToBeanNames.get(name);
        if (beanNames == null){
            beanNames = new HashSet<>();
            referenceHasInit.put(name, Boolean.FALSE);
        }
        beanNames.add(beanName);
        referenceIdToBeanNames.put(name, beanNames);
    }

    private AnnotationAttributes findAnnotation(AccessibleObject ao, Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
        if (ao.getAnnotations().length > 0) {
            for (Class<? extends Annotation> type : autowiredAnnotationTypes) {
                // same effects with AnnotatedElementUtils.getMergedAnnotation
                AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ao, type);
                if (attributes != null) {
                    return attributes;
                }
            }
        }
        return null;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for(String beanName : beanNames){
            Class<?> clazz = beanFactory.getType(beanName);
            if (clazz != null){
                buildReferenceMetadata(clazz, beanName);
            }else {
                logger.debug(beanName + " class not found");
            }
        }
    }


    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        initReference(beanType, beanName);
    }

    private void initReference(Class<?> beanType, String beanName) {
        findOrInjectAnnotation(beanType, (InjectCallback) this::doInitReference, beanName, this.autowiredAnnotationTypes, Autowired.class.getName());
    }

    /**
     * call getBean
     * @param name the ReferenceClass name.
     * @param beanName bean wait to init
     */
    private void doInitReference(String name, String beanName) {
        if (this.referenceIdToBeanNames.containsKey(name) && !referenceHasInit.get(name)){
            Set<String> beanNames = referenceIdToBeanNames.get(name);
            if(beanNames.contains(beanName)){
                referenceHasInit.put(name, Boolean.TRUE);
                return;
            }
            beanNames.forEach(t->beanFactory.getBean(t));
            referenceHasInit.put(name, Boolean.TRUE);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "ReferenceBeanFactoryPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    interface CallBack{
        void callback(String name, String beanName);
    }

    interface FindCallback extends CallBack{

    }
    interface InjectCallback extends CallBack{

    }
}
