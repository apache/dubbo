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
package org.apache.dubbo.config.spring.propertyconfigurer.consumer3;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.propertyconfigurer.consumer.DemoBeanFactoryPostProcessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PropertySourcesInJavaConfigTest {

    private static final String SCAN_PACKAGE_NAME = "org.apache.dubbo.config.spring.propertyconfigurer.consumer3.notexist";
    private static final String PACKAGE_PATH = "/org/apache/dubbo/config/spring/propertyconfigurer/consumer3";
    private static final String PROVIDER_CONFIG_PATH = "org/apache/dubbo/config/spring/propertyconfigurer/provider/dubbo-provider.xml";

    @BeforeEach
    public void setUp() throws Exception {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
    }

    @BeforeEach
    public void beforeTest() {
        DubboBootstrap.reset();
    }

    @Test
    public void testImportPropertySource() {

        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(PROVIDER_CONFIG_PATH);
        try {
            providerContext.start();

            // Resolve placeholder by import property sources
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class, ImportPropertyConfiguration.class);
            try {
                // expect auto create PropertySourcesPlaceholderConfigurer bean
                String[] beanNames = context.getBeanNamesForType(PropertySourcesPlaceholderConfigurer.class);
                Assertions.assertEquals(1, beanNames.length);
                Assertions.assertEquals(PropertySourcesPlaceholderConfigurer.class.getName(), beanNames[0]);

                HelloService service = (HelloService) context.getBean("demoService");
                String result = service.sayHello("world");
                System.out.println("result: " + result);
                Assertions.assertEquals("Hello world, response from provider: " + InetSocketAddress.createUnresolved("127.0.0.1", 0), result);
            } finally {
                context.close();
            }

        } finally {
            providerContext.close();
        }
    }

    @Test
    public void testCustomPropertySourceBean() {

        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(PROVIDER_CONFIG_PATH);
        try {
            providerContext.start();

            // Resolve placeholder by custom PropertySourcesPlaceholderConfigurer bean
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class, PropertyBeanConfiguration.class);
            try {
                // expect using custom PropertySourcesPlaceholderConfigurer bean
                String[] beanNames = context.getBeanNamesForType(PropertySourcesPlaceholderConfigurer.class);
                Assertions.assertEquals(1, beanNames.length);
                Assertions.assertEquals("myPropertySourcesPlaceholderConfigurer", beanNames[0]);

                HelloService service = (HelloService) context.getBean("demoService");
                String result = service.sayHello("world");
                System.out.println("result: " + result);
                Assertions.assertEquals("Hello world, response from provider: " + InetSocketAddress.createUnresolved("127.0.0.1", 0), result);
            } finally {
                context.close();
            }

        } finally {
            providerContext.close();
        }
    }

    @Configuration
    @EnableDubbo(scanBasePackages = SCAN_PACKAGE_NAME)
    @ComponentScan(value = {SCAN_PACKAGE_NAME})
    @ImportResource("classpath:" + PACKAGE_PATH + "/dubbo-consumer.xml")
    static class ConsumerConfiguration {
        @Bean
        public DemoBeanFactoryPostProcessor bizBeanFactoryPostProcessor(HelloService service) {
            return new DemoBeanFactoryPostProcessor(service);
        }
    }

    @Configuration
    @PropertySource("classpath:" + PACKAGE_PATH + "/app.properties")
    static class ImportPropertyConfiguration {

    }

    @Configuration
    static class PropertyBeanConfiguration {
        @Bean
        public MyPropertySourcesPlaceholderConfigurer myPropertySourcesPlaceholderConfigurer() {
            MyPropertySourcesPlaceholderConfigurer placeholderConfigurer = new MyPropertySourcesPlaceholderConfigurer();
            placeholderConfigurer.setLocation(new ClassPathResource(PACKAGE_PATH + "/app.properties"));
            return placeholderConfigurer;
        }
    }

    static class MyPropertySourcesPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {
        @Override
        protected String convertProperty(String propertyName, String propertyValue) {
            // .. do something ..
            return propertyValue;
        }
    }
}
