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
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DubboConfigBeanDefinitionConflictApplicationListener} Test
 *
 * @since 2.7.5
 */
class DubboConfigBeanDefinitionConflictApplicationListenerTest {

    @BeforeEach
    void init() {
        DubboBootstrap.reset();
        // context.addApplicationListener(new DubboConfigBeanDefinitionConflictApplicationListener());
    }

    @AfterEach
    void destroy() {
        DubboBootstrap.reset();
    }

    // @Test
    void testNormalCase() {

        System.setProperty("dubbo.application.name", "test-dubbo-application");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DubboConfig.class);
        try {
            context.start();

            ApplicationConfig applicationConfig = context.getBean(ApplicationConfig.class);

            assertEquals("test-dubbo-application", applicationConfig.getName());
        } finally {
            System.clearProperty("dubbo.application.name");
            context.close();
        }
    }

    @Test
    void testDuplicatedConfigsCase() {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(PropertySourceConfig.class, DubboConfig.class, XmlConfig.class);

        try {
            context.start();

            Map<String, ApplicationConfig> beansMap = context.getBeansOfType(ApplicationConfig.class);

            ApplicationConfig applicationConfig = beansMap.get("dubbo-consumer-2.7.x");

            assertEquals(1, beansMap.size());

            assertEquals("dubbo-consumer-2.7.x", applicationConfig.getName());
        } finally {
            context.close();
        }
    }

    @EnableDubboConfig
    static class DubboConfig {}

    @PropertySource("classpath:/META-INF/dubbo.properties")
    static class PropertySourceConfig {}

    @ImportResource("classpath:/META-INF/spring/dubbo-context.xml")
    static class XmlConfig {}
}
