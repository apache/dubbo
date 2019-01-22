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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Annotated {@link BeanDefinition} Utilities
 * <p>
 * The source code is cloned from https://github.com/alibaba/spring-context-support/blob/1.0.2/src/main/java/com/alibaba/spring/util/AnnotatedBeanDefinitionRegistryUtils.java
 * @since 2.6.6
 */
public abstract class AnnotatedBeanDefinitionRegistryUtils {

    private static final Log logger = LogFactory.getLog(AnnotatedBeanDefinitionRegistryUtils.class);

    /**
     * Register Beans
     *
     * @param registry         {@link BeanDefinitionRegistry}
     * @param annotatedClasses {@link Annotation annotation} class
     */
    public static void registerBeans(BeanDefinitionRegistry registry, Class<?>... annotatedClasses) {

        if (ObjectUtils.isEmpty(annotatedClasses)) {
            return;
        }

        boolean debugEnabled = logger.isDebugEnabled();

        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);

        if (debugEnabled) {
            logger.debug(registry.getClass().getSimpleName() + " will register annotated classes : " + Arrays.asList(annotatedClasses) + " .");
        }

        reader.register(annotatedClasses);

    }
}
