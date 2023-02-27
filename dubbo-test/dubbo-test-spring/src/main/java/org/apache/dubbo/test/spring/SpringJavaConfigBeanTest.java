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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;
import org.apache.dubbo.test.common.SysProps;
import org.apache.dubbo.test.common.api.DemoService;
import org.apache.dubbo.test.common.impl.DemoServiceImpl;
import org.apache.dubbo.test.spring.context.MockSpringInitCustomizer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Map;

public class SpringJavaConfigBeanTest {

    private static final String MY_PROTOCOL_ID = "myProtocol";
    private static final String MY_REGISTRY_ID = "my-registry";

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll(){
        DubboBootstrap.reset();
    }

    @BeforeEach
    public void beforeEach() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @Test
    public void testBean() {

        SysProps.setProperty("dubbo.application.owner", "Tom");
        SysProps.setProperty("dubbo.application.qos-enable", "false");
        SysProps.setProperty("dubbo.protocol.name", "dubbo");
        SysProps.setProperty("dubbo.protocol.port", "2346");
        String registryAddress = ZookeeperRegistryCenterConfig.getConnectionAddress();
        SysProps.setProperty("dubbo.registry.address", registryAddress);
        SysProps.setProperty("dubbo.provider.group", "test");

        AnnotationConfigApplicationContext consumerContext = new AnnotationConfigApplicationContext(
                TestConfiguration.class, ConsumerConfiguration.class, ProviderConfiguration.class);
        try {
            consumerContext.start();

            ApplicationModel applicationModel = consumerContext.getBean(ApplicationModel.class);
            ConfigManager configManager = consumerContext.getBean(ConfigManager.class);
            ApplicationConfig application = configManager.getApplication().get();
            Assertions.assertEquals(false, application.getQosEnable());
            Assertions.assertEquals("Tom", application.getOwner());

            RegistryConfig registry = configManager.getRegistry(MY_REGISTRY_ID).get();
            Assertions.assertEquals(registryAddress, registry.getAddress());

            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(1, protocols.size());
            ProtocolConfig protocolConfig = protocols.iterator().next();
            Assertions.assertEquals("dubbo", protocolConfig.getName());
            Assertions.assertEquals(2346, protocolConfig.getPort());
            Assertions.assertEquals(MY_PROTOCOL_ID, protocolConfig.getId());

            ModuleConfigManager moduleConfigManager = applicationModel.getDefaultModule().getConfigManager();
            ConsumerConfig consumerConfig = moduleConfigManager.getDefaultConsumer().get();
            Assertions.assertEquals(1000, consumerConfig.getTimeout());
            Assertions.assertEquals("demo", consumerConfig.getGroup());
            Assertions.assertEquals(false, consumerConfig.isCheck());
            Assertions.assertEquals(2, consumerConfig.getRetries());

            Map<String, ReferenceBean> referenceBeanMap = consumerContext.getBeansOfType(ReferenceBean.class);
            Assertions.assertEquals(1, referenceBeanMap.size());
            ReferenceBean referenceBean = referenceBeanMap.get("&demoService");
            Assertions.assertNotNull(referenceBean);
            ReferenceConfig referenceConfig = referenceBean.getReferenceConfig();
            // use consumer's attributes as default value
            Assertions.assertEquals(consumerConfig.getTimeout(), referenceConfig.getTimeout());
            Assertions.assertEquals(consumerConfig.getGroup(), referenceConfig.getGroup());
            // consumer cannot override reference's attribute
            Assertions.assertEquals(5, referenceConfig.getRetries());

            DemoService referProxy = (DemoService) referenceConfig.get();
            Assertions.assertTrue( referProxy instanceof DemoService);
            String result = referProxy.sayHello("dubbo");
            Assertions.assertEquals("Hello dubbo", result);

            // check initialization customizer
            MockSpringInitCustomizer.checkCustomizer(consumerContext);
        } finally {
            consumerContext.close();
        }

    }


    @EnableDubbo(scanBasePackages = "")
    @Configuration
    static class TestConfiguration {

        @Bean(name = "dubbo-demo-application")
        public ApplicationConfig applicationConfig() {
            ApplicationConfig applicationConfig = new ApplicationConfig();
            applicationConfig.setName("dubbo-demo-application");
            return applicationConfig;
        }

        @Bean(name = MY_PROTOCOL_ID)
        public ProtocolConfig protocolConfig() {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setName("rest");
            protocolConfig.setPort(1234);
            return protocolConfig;
        }

        @Bean(name = MY_REGISTRY_ID)
        public RegistryConfig registryConfig() {
            RegistryConfig registryConfig = new RegistryConfig();
            registryConfig.setAddress("N/A");
            return registryConfig;
        }

        @Bean
        public ConsumerConfig consumerConfig() {
            ConsumerConfig consumer = new ConsumerConfig();
            consumer.setTimeout(1000);
            consumer.setGroup("demo");
            consumer.setCheck(false);
            consumer.setRetries(2);
            return consumer;
        }
    }

    @Configuration
    static class ConsumerConfiguration {

        @Bean
        @DubboReference(scope = Constants.SCOPE_LOCAL, retries = 5)
        public ReferenceBean<DemoService> demoService() {
            return new ReferenceBean<>();
        }

    }

    @Configuration
    static class ProviderConfiguration {

        @Bean
        @DubboService(group = "demo")
        public DemoService demoServiceImpl() {
            return new DemoServiceImpl();
        }
    }
}
