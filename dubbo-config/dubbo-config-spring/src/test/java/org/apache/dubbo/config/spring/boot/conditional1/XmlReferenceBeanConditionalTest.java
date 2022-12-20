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
package org.apache.dubbo.config.spring.boot.conditional1;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.annotation.Order;

import java.util.Map;

/**
 * issue: https://github.com/apache/dubbo-spring-boot-project/issues/779
 */
@SpringBootTest(
        properties = {
                "dubbo.registry.address=N/A"
        },
        classes = {
                XmlReferenceBeanConditionalTest.class
        }
)
@Configuration
//@ComponentScan
class XmlReferenceBeanConditionalTest {

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
    void testConsumer() {

        Map<String, HelloService> helloServiceMap = applicationContext.getBeansOfType(HelloService.class);
        Assertions.assertEquals(1, helloServiceMap.size());
        Assertions.assertNotNull(helloServiceMap.get("helloService"));
        Assertions.assertNull(helloServiceMap.get("myHelloService"));
    }

    @Order(Integer.MAX_VALUE-2)
    @Configuration
    @ImportResource("classpath:/org/apache/dubbo/config/spring/boot/conditional1/consumer/dubbo-consumer.xml")
    public static class ConsumerConfiguration {

    }

    @Order(Integer.MAX_VALUE-1)
    @Configuration
    public static class ConsumerConfiguration2 {

        //TEST Conditional, this bean should be ignored
        @Bean
        @ConditionalOnMissingBean
        public HelloService myHelloService() {
            return new HelloService() {
                @Override
                public String sayHello(String name) {
                    return "HI, " + name;
                }
            };
        }
    }
}