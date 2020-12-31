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
package org.apache.dubbo.config.spring.context.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;

/**
 * {@link DubboConfigConfiguration} Test
 *
 * @since 2.5.8
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DubboConfigConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void before() throws IOException {

        context = new AnnotationConfigApplicationContext();
        ResourcePropertySource propertySource = new ResourcePropertySource("META-INF/config.properties");
        context.getEnvironment().getPropertySources().addFirst(propertySource);

    }

    @AfterEach
    public void after() {
        context.close();
    }

    @Test
    public void testSingle() throws IOException {

        context.register(DubboConfigConfiguration.Single.class);
        context.refresh();

        // application
        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());

        // module
        ModuleConfig moduleConfig = context.getBean("moduleBean", ModuleConfig.class);
        Assertions.assertEquals("dubbo-demo-module", moduleConfig.getName());

        // registry
        RegistryConfig registryConfig = context.getBean(RegistryConfig.class);
        Assertions.assertEquals("zookeeper://192.168.99.100:32770", registryConfig.getAddress());

        // protocol
        ProtocolConfig protocolConfig = context.getBean(ProtocolConfig.class);
        Assertions.assertEquals("dubbo", protocolConfig.getName());
        Assertions.assertEquals(Integer.valueOf(20880), protocolConfig.getPort());
    }

    @Test
    public void testMultiple() {

        context.register(DubboConfigConfiguration.Multiple.class);
        context.refresh();

        // application
        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());

        ApplicationConfig applicationBean2 = context.getBean("applicationBean2", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application2", applicationBean2.getName());

        ApplicationConfig applicationBean3 = context.getBean("applicationBean3", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application3", applicationBean3.getName());

    }

}
