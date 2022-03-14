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
package org.apache.dubbo.test.spring;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.test.common.api.DemoService;
import org.apache.dubbo.test.spring.context.MockSpringInitCustomizer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

public class SpringAnnotationBeanTest {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll(){
        DubboBootstrap.reset();
    }

    @Test
    public void test() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestConfiguration.class);
        TestService testService = applicationContext.getBean(TestService.class);
        testService.test();

        // check initialization customizer
        MockSpringInitCustomizer.checkCustomizer(applicationContext);
    }

    @EnableDubbo(scanBasePackages = "org.apache.dubbo.test.common.impl")
    @Configuration
    @PropertySource("/demo-app.properties")
    static class TestConfiguration {

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    static class TestService {

        @DubboReference
        private DemoService demoService;

        public void test() {
            String result = demoService.sayHello("dubbo");
            Assertions.assertEquals("Hello dubbo", result);
        }
    }
}
