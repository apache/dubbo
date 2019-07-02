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
package org.apache.dubbo.config.spring.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * Annotated {@link BeanDefinition} Utilities
 * <p>
 * The source code is cloned from https://github.com/alibaba/spring-context-support/blob/1.0.2/src/main/java/com/alibaba/spring/util/AnnotatedBeanDefinitionRegistryUtils.java
 *
 * @since 2.6.6
 */
public abstract class AnnotatedBeanDefinitionRegistryUtils {

    private static final Log logger = LogFactory.getLog(AnnotatedBeanDefinitionRegistryUtils.class);

    /**
     * Is present bean that was registered by the specified {@link Annotation annotated} {@link Class class}
     *
     * @param registry       {@link BeanDefinitionRegistry}
     * @param annotatedClass the {@link Annotation annotated} {@link Class class}
     * @return if present, return <code>true</code>, or <code>false</code>
     * @since 2.7.3
     */
    public static boolean isPresentBean(BeanDefinitionRegistry registry, Class<?> annotatedClass) {

        boolean present = false;

        String[] beanNames = registry.getBeanDefinitionNames();

        ClassLoader classLoader = annotatedClass.getClassLoader();

        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                AnnotationMetadata annotationMetadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
                String className = annotationMetadata.getClassName();
                Class<?> targetClass = resolveClassName(className, classLoader);
                present = Objects.equals(targetClass, annotatedClass);
                if (present) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(format("The annotatedClass[class : %s , bean name : %s] was present in registry[%s]",
                                className, beanName, registry));
                    }
                    break;
                }
            }
        }

        return present;
    }

    /**
     * Register Beans if not present in {@link BeanDefinitionRegistry registry}
     *
     * @param registry         {@link BeanDefinitionRegistry}
     * @param annotatedClasses {@link Annotation annotation} class
     * @revision 2.7.3 {@link #isPresentBean(BeanDefinitionRegistry, Class)}
     */
    public static void registerBeans(BeanDefinitionRegistry registry, Class<?>... annotatedClasses) {

        if (ObjectUtils.isEmpty(annotatedClasses)) {
            return;
        }

        boolean debugEnabled = logger.isDebugEnabled();

        // Remove all annotated-classes that have been registered
        Iterator<Class<?>> iterator = new ArrayList<>(asList(annotatedClasses)).iterator();

        while (iterator.hasNext()) {
            Class<?> annotatedClass = iterator.next();
            if (isPresentBean(registry, annotatedClass)) {
                iterator.remove();
            }
        }

        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);

        if (debugEnabled) {
            logger.debug(registry.getClass().getSimpleName() + " will register annotated classes : " + asList(annotatedClasses) + " .");
        }

        reader.register(annotatedClasses);

    }
}
