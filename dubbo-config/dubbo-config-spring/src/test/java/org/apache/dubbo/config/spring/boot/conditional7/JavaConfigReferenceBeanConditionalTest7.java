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
package org.apache.dubbo.config.spring.boot.conditional7;

import java.util.Map;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.context.annotation.provider.HelloServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * issue: https://github.com/apache/dubbo-spring-boot-project/issues/779
 *
 * use @Component with @Bean method to config ReferenceBean
 *
 */
@SpringBootTest(
    properties = {
        "dubbo.application.name=consumer-app",
        "dubbo.registry.address=N/A",
        "myapp.group=demo"
    },
    classes = {
        JavaConfigReferenceBeanConditionalTest7.class
    }
)
@Configuration
@EnableDubbo
public class JavaConfigReferenceBeanConditionalTest7 {

    @BeforeAll
    public static void beforeAll(){
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll(){
        DubboBootstrap.reset();
    }

    @Autowired
    private HelloService helloService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testConsumer() {

        Map<String, HelloService> helloServiceMap = applicationContext.getBeansOfType(HelloService.class);
        Assertions.assertEquals(1, helloServiceMap.size());
        Assertions.assertNull(helloServiceMap.get("helloServiceImpl"));
        HelloService helloService = helloServiceMap.get("helloService");
        Assertions.assertNotNull(helloService);
        Assertions.assertFalse(helloService instanceof HelloServiceImpl, "Not expected bean type: "+helloService.getClass());
    }

    // make sure that the one using condition runs after.
    @Order(Integer.MAX_VALUE-1)
    @Configuration
    // 注解添加到这个类上，保证先扫描，再load bean method
    @ComponentScan(basePackages = "org.apache.dubbo.config.spring.boot.conditional7.consumer")
    public static class ServiceBeanConfiguration {

        @Bean
        @ConditionalOnMissingBean(HelloService.class)
        public HelloService helloServiceImpl() {
            return new HelloServiceImpl();
        }
    }
}
