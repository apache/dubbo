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
package org.apache.dubbo.config.spring.reference;


import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.DubboConfigBeanInitializer;
import org.apache.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link org.apache.dubbo.config.spring.context.DubboConfigBeanInitializer}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        DubboConfigBeanInitializerTest.class,
        DubboConfigBeanInitializerTest.AppConfiguration.class,
    })
@TestPropertySource(properties = {
    "dubbo.protocol.port=-1",
    "dubbo.registry.address=${zookeeper.connection.address}"
})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DubboConfigBeanInitializerTest {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }


    @Autowired
    private FooService fooService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void test() {
        Assertions.assertNotNull(fooService, "fooService is null");
        Assertions.assertNotNull(fooService.helloService, "ooService.helloService is null");

        // expect fooService is registered before dubbo config bean
        List<String> beanNames = Arrays.asList(applicationContext.getBeanDefinitionNames());
        int fooServiceIndex = beanNames.indexOf("fooService");
        int applicationConfigIndex = beanNames.indexOf("dubbo-demo-application");
        int registryConfigIndex = beanNames.indexOf("my-registry");
        int configInitializerIndex = beanNames.indexOf(DubboConfigBeanInitializer.BEAN_NAME);
        Assertions.assertTrue(fooServiceIndex < applicationConfigIndex);
        Assertions.assertTrue(fooServiceIndex < registryConfigIndex);
        Assertions.assertTrue(fooServiceIndex < configInitializerIndex);
    }

    @Configuration
    // Import BusinessConfig first, make sure FooService bean is register early,
    // expect dubbo config beans are initialized before FooService bean
    @Import({BusinessConfig.class, ConsumerConfig.class, ProviderConfiguration.class})
    static class AppConfiguration {

    }

    @Configuration
    static class BusinessConfig {
        @Bean
        public FooService fooService(ApplicationContext applicationContext) {
            // Dubbo config beans should be inited at DubboConfigInitializer, before init FooService bean
            Assertions.assertTrue(DubboBeanUtils.getModuleModel(applicationContext).getDeployer().isInitialized());
            Assertions.assertTrue(DubboBeanUtils.getApplicationModel(applicationContext).getDeployer().isInitialized());
            Assertions.assertTrue(DubboBootstrap.getInstance().isInitialized());
            return new FooService();
        }
    }

    @Configuration
    static class ConsumerConfig {
        @DubboReference
        private HelloService helloService;
    }

    static class FooService {
        @Autowired
        private HelloService helloService;
    }
}
