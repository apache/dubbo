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

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.annotation.consumer.lazyinit.DefaultLazyInitConsumer;
import org.apache.dubbo.config.spring.annotation.consumer.lazyinit.LazyInitConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
                LazyInitDubboAnnotationTest.class,
                DefaultLazyInitConsumer.class
        })
@TestPropertySource(properties = {
        "consumer.package = org.apache.dubbo.config.spring.annotation.consumer.lazyinit",
        "packagesToScan = ${consumer.package}",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LazyInitDubboAnnotationTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() {
        DubboBootstrap.reset();
    }

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Bean
    public ServiceClassPostProcessor serviceClassPostProcessor(@Value("${packagesToScan}") String... packagesToScan) {
        return new ServiceClassPostProcessor(packagesToScan);
    }

    /**
     * lazy-init Application Configuration
     *
     * @return {@link ApplicationConfig} Bean
     */
    @Bean("applicationConfig")
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("lazy-init-annotation-application");
        return applicationConfig;
    }

    /**
     * lazy-init Registry Configuration
     *
     * @return {@link RegistryConfig} Bean
     */
    @Bean("registryConfig")
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("N/A");
        return registryConfig;
    }

    /**
     * lazy-init Protocol Configuration
     *
     * @return {@link ProtocolConfig} Bean
     */
    @Bean("protocolConfig")
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(NetUtils.getAvailablePort());
        return protocolConfig;
    }

    @Test
    public void testLazyInitReference() {
        // Customer has Lazy annotation
        BeanDefinition defaultLazyInitConsumerBeanDefinition = beanFactory.getBeanDefinition("defaultLazyInitConsumer");
        Assertions.assertEquals(defaultLazyInitConsumerBeanDefinition.isLazyInit(), true);

        // Service has Lazy annotation
        BeanDefinition defaultLazyInitServiceBeanDefinition = beanFactory.getBeanDefinition("defaultLazyInitService");
        Assertions.assertEquals(defaultLazyInitServiceBeanDefinition.isLazyInit(), true);

        // Service doesn't have Lazy annotation
        BeanDefinition defaultNotLazyInitServiceBeanDefinition = beanFactory.getBeanDefinition("defaultNotLazyInitService");
        Assertions.assertEquals(defaultNotLazyInitServiceBeanDefinition.isLazyInit(), false);

        // Call Customer's reference and it will initialize
        try {
            beanFactory.getBean("defaultLazyInitConsumer", LazyInitConsumer.class);
        } catch (Exception ex) {
            // throw an exception because the provider need to export, but the registry config is invalid.
            Throwable rootCause = ex.getCause();
            Assertions.assertTrue(rootCause instanceof IllegalStateException);
            Assertions.assertTrue(rootCause.getMessage().startsWith("No registry config found or it's not a valid config!"));
        }
    }

}
