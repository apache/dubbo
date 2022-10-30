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
package org.apache.dubbo.spring.boot.context.event;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;


/**
 * {@link DubboConfigBeanDefinitionConflictApplicationListener} Test
 *
 * @since 2.7.5
 */
public class DubboConfigBeanDefinitionConflictApplicationListenerTest {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Before
    public void init() {
        ApplicationModel.reset();
        context.addApplicationListener(new DubboConfigBeanDefinitionConflictApplicationListener());
    }

    @After
    public void destroy() {
        context.close();
        ApplicationModel.reset();

    }

    @Test
    public void testNormalCase() {

        System.setProperty("dubbo.application.name", "test-dubbo-application");

        context.register(DubboConfig.class);

        context.refresh();

        ApplicationConfig applicationConfig = context.getBean(ApplicationConfig.class);

        Assert.assertEquals("test-dubbo-application", applicationConfig.getName());
    }

    @Test
    public void testDuplicatedConfigsCase() {

        context.register(PropertySourceConfig.class, DubboConfig.class);

        context.register(XmlConfig.class);

        context.refresh();

        Map<String, ApplicationConfig> beansMap = context.getBeansOfType(ApplicationConfig.class);

        ApplicationConfig applicationConfig = beansMap.get("dubbo-consumer-2.7.x");

        Assert.assertEquals(1, beansMap.size());

        Assert.assertEquals("dubbo-consumer-2.7.x", applicationConfig.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testFailedCase() {
        context.register(ApplicationConfig.class);
        testDuplicatedConfigsCase();
    }

    @EnableDubboConfig
    static class DubboConfig {

    }

    @PropertySource("classpath:/META-INF/dubbo.properties")
    static class PropertySourceConfig {

    }

    @ImportResource("classpath:/META-INF/spring/dubbo-context.xml")
    static class XmlConfig {
    }
}
