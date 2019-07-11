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

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import static org.apache.dubbo.config.spring.util.AnnotatedBeanDefinitionRegistryUtils.isPresentBean;
import static org.apache.dubbo.config.spring.util.AnnotatedBeanDefinitionRegistryUtils.registerBeans;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AnnotatedBeanDefinitionRegistryUtils} Test
 *
 * @since 2.7.3
 */
public class AnnotatedBeanDefinitionRegistryUtilsTest {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @AfterEach
    public void destroy() {
        context.close();
    }

    @Test
    public void testIsPresentBean() {

        assertFalse(isPresentBean(context, A.class));

        context.register(A.class);

        for (int i = 0; i < 9; i++) {
            assertTrue(isPresentBean(context, A.class));
        }

    }

    @Test
    public void testRegisterBeans() {

        registerBeans(context, A.class, A.class);

        context.refresh();

        A a = context.getBean(A.class);

        Assert.assertNotNull(a);
    }


    @Service
    static class A {

    }
}
