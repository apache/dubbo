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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.api.DemoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ReferenceBeanFactoryPostProcessor} Test
 *
 * @author anLA7856
 */
public class ReferenceBeanFactoryPostProcessorTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void setUp() {
        context = new AnnotationConfigApplicationContext();
        registerApplicationConfig();
        registerRegistryConfig();
        registerProtocolConfig();
        setProperty();
    }

    private void setProperty() {
        MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        Map<String, Object> map = new HashMap<>();
        map.put("demo.service.version","2.5.7");
        map.put("demo.service.application", "dubbo-demo-application");
        map.put("demo.service.protocol", "dubbo");
        map.put("demo.service.registry", "my-registry");
        propertySources
                .addFirst(new MapPropertySource("newmap", map));
    }

    @AfterEach
    public void tearDown() {
        context.close();
    }

    @Test
    public void testAutowired() {
        context.register(ReferenceAnnotationBeanPostProcessor.class,
                ReferenceBeanFactoryPostProcessor.class,
                TestAutowiredConfig.class,
                TestReferenceConfig.class);
        context.refresh();
        context.start();
        TestAutowiredConfig testAutowiredConfig = context.getBean(TestAutowiredConfig.class);
        Assertions.assertNotNull(testAutowiredConfig);
        Assertions.assertNotNull(testAutowiredConfig.getDemoService());
    }

    @Test
    public void testResource() {
        context.register(ReferenceAnnotationBeanPostProcessor.class,
                ReferenceBeanFactoryPostProcessor.class,
                TestResourceConfig.class,
                TestReferenceConfig.class);
        context.refresh();
        context.start();
        TestResourceConfig testResourceConfig = context.getBean(TestResourceConfig.class);
        Assertions.assertNotNull(testResourceConfig);
        Assertions.assertNotNull(testResourceConfig.getDemoService());
    }

    @Test
    public void testWithoutReferenceBeanFactoryPostProcessor() {
        context.register(ReferenceAnnotationBeanPostProcessor.class,
                TestAutowiredConfig.class,
                TestReferenceConfig.class);
        // UnsatisfiedDependencyException or NoSuchBeanDefinitionException
        Exception exception = assertThrows(Exception.class, () -> {
            context.refresh();
        });
        String expectedMessage = "Exception";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }
    
    static class TestReferenceConfig{
        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345?version=2.5.7", check = false)
        DemoService demoService;
    }

    static class TestAutowiredConfig{
        @Autowired
        DemoService demoService;

        public DemoService getDemoService() {
            return demoService;
        }
    }

    static class TestResourceConfig{
        @Resource
        DemoService demoService;

        public DemoService getDemoService() {
            return demoService;
        }
    }


    public void registerApplicationConfig() {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(ApplicationConfig.class);
        definition.getPropertyValues().add("name",  "dubbo-demo-application");
        context.registerBeanDefinition("dubbo-demo-application", definition);
    }

    public void registerRegistryConfig() {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(RegistryConfig.class);
        definition.getPropertyValues().add("address",  "N/A");
        context.registerBeanDefinition("my-registry", definition);
    }

    public void registerProtocolConfig() {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(ProtocolConfig.class);
        definition.getPropertyValues().add("name",  "dubbo").add("port","12345");
        context.registerBeanDefinition("dubbo", definition);
    }

}