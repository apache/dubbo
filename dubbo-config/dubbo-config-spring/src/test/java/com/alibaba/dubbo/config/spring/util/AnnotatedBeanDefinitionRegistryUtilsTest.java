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
package com.alibaba.dubbo.config.spring.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link AnnotatedBeanDefinitionRegistryUtils} Test
 *
 * @see AnnotatedBeanDefinitionRegistryUtils
 * @since 2.6.6
 */
public class AnnotatedBeanDefinitionRegistryUtilsTest {

    private DefaultListableBeanFactory registry = null;

    @Before
    public void init() {
        registry = new DefaultListableBeanFactory();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
    }

    @Test
    public void testRegisterBeans() {

        AnnotatedBeanDefinitionRegistryUtils.registerBeans(registry, this.getClass());

        String[] beanNames = registry.getBeanNamesForType(this.getClass());

        Assert.assertEquals(1, beanNames.length);

        beanNames = registry.getBeanNamesForType(AnnotatedBeanDefinitionRegistryUtils.class);

        Assert.assertTrue(ObjectUtils.isEmpty(beanNames));

        AnnotatedBeanDefinitionRegistryUtils.registerBeans(registry);

    }

}
