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
package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

/**
 * {@link DubboConfigBindingRegistrar}
 *
 * @since 2.5.8
 */
public class DubboConfigBindingRegistrarTest {

    @Test
    public void testRegisterBeanDefinitionsForSingle() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(TestApplicationConfig.class);

        context.refresh();

        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());


    }

    @Test
    public void testRegisterBeanDefinitionsForMultiple() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(TestMultipleApplicationConfig.class);

        context.refresh();

        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());

        applicationConfig = context.getBean("applicationBean2", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application2", applicationConfig.getName());

        applicationConfig = context.getBean("applicationBean3", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application3", applicationConfig.getName());


    }

    @EnableDubboConfigBinding(prefix = "${application.prefixes}", type = ApplicationConfig.class, multiple = true)
    @PropertySource("META-INF/config.properties")
    private static class TestMultipleApplicationConfig {

    }

    @EnableDubboConfigBinding(prefix = "${application.prefix}", type = ApplicationConfig.class)
    @PropertySource("META-INF/config.properties")
    private static class TestApplicationConfig {

    }


}
