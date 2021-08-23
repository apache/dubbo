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
package org.apache.dubbo.config.spring.propertyconfigurer.consumer;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.registrycenter.ZookeeperSingleRegistryCenter;
import org.apache.dubbo.config.spring.registrycenter.RegistryCenter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class PropertyConfigurerTest {

    private static RegistryCenter singleRegistryCenter;

    @BeforeAll
    public static void beforeAll() {
        singleRegistryCenter = new ZookeeperSingleRegistryCenter();
        singleRegistryCenter.startup();
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
        singleRegistryCenter.shutdown();
    }

    @Test
    public void testEarlyInit() {

        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext("org/apache/dubbo/config/spring/propertyconfigurer/provider/dubbo-provider.xml");
        try {
            providerContext.start();

            ConcurrentMap<String, Set<URL>> tmp = ApplicationModel.getServiceRepository().getProviderUrlsWithoutGroup();
            // reset ConfigManager of provider context
            DubboBootstrap.reset(false);
            ApplicationModel.getServiceRepository().setProviderUrlsWithoutGroup(tmp);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            // Resolve placeholder by PropertyPlaceholderConfigurer in dubbo-consumer.xml, without import property source.
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
            context.start();

            HelloService service = (HelloService) context.getBean("demoService");
            String result = service.sayHello("world");
            System.out.println("result: " + result);
            Assertions.assertEquals("Hello world, response from provider: 127.0.0.1:0", result);

            context.close();

        } finally {
            providerContext.close();
        }

    }

    @Configuration
    @EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.propertyconfigurer.consumer")
    @ComponentScan(value = {"org.apache.dubbo.config.spring.propertyconfigurer.consumer"})
    @ImportResource("classpath:/org/apache/dubbo/config/spring/propertyconfigurer/consumer/dubbo-consumer.xml")
    static class ConsumerConfiguration {
        @Bean
        public DemoBeanFactoryPostProcessor bizBeanFactoryPostProcessor(HelloService service) {
            return new DemoBeanFactoryPostProcessor(service);
        }
    }
}
