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
package org.apache.dubbo.config.spring.boot.importxml;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@SpringBootTest(
        properties = {
                "dubbo.registry.protocol=zookeeper",
                "dubbo.registry.address=localhost:2181"
        },
        classes = {
                SpringBootImportDubboXmlTest.class
        }
)
@Configuration
@ComponentScan
@ImportResource("classpath:/org/apache/dubbo/config/spring/boot/importxml/consumer/dubbo-consumer.xml")
public class SpringBootImportDubboXmlTest {

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

    @Test
    public void testConsumer() {
        try {
            helloService.sayHello("dubbo");
            Assertions.fail("Should not be called successfully");
        } catch (Exception e) {
            String s = e.toString();
            Assertions.assertTrue(s.contains("No provider available"), s);
            Assertions.assertTrue(s.contains("service org.apache.dubbo.config.spring.api.HelloService"), s);
        }
    }

}
