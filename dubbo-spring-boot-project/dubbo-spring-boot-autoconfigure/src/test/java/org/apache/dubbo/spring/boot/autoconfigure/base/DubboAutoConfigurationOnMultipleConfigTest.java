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
package org.apache.dubbo.spring.boot.autoconfigure.base;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DubboAutoConfiguration} Test On multiple Dubbo Configuration
 *
 * @since 2.7.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(
        properties = {
            "dubbo.applications.application1.name=dubbo-demo-multi-application",
            "dubbo.modules.module1.name=dubbo-demo-module",
            "dubbo.registries.registry1.address=test://192.168.99.100:32770",
            "dubbo.protocols.protocol1.name=dubbo",
            "dubbo.protocols.protocol1.port=20880",
            "dubbo.monitors.monitor1.address=test://127.0.0.1:32770",
            "dubbo.providers.provider1.host=127.0.0.1",
            "dubbo.consumers.consumer1.client=netty",
            "dubbo.config.multiple=true",
            "dubbo.scan.basePackages=org.apache.dubbo.spring.boot.dubbo, org.apache.dubbo.spring.boot.condition"
        })
@SpringBootTest(classes = {DubboAutoConfigurationOnMultipleConfigTest.class})
@EnableAutoConfiguration
@ComponentScan
class DubboAutoConfigurationOnMultipleConfigTest {

    /**
     * @see TestBeansConfiguration
     */
    @Autowired
    @Qualifier("application1")
    ApplicationConfig application;

    @Autowired
    @Qualifier("module1")
    ModuleConfig module;

    @Autowired
    @Qualifier("registry1")
    RegistryConfig registry;

    @Autowired
    @Qualifier("monitor1")
    MonitorConfig monitor;

    @Autowired
    @Qualifier("protocol1")
    ProtocolConfig protocol;

    @Autowired
    @Qualifier("consumer1")
    ConsumerConfig consumer;

    @Autowired
    @Qualifier("provider1")
    ProviderConfig provider;

    @BeforeEach
    void init() {
        DubboBootstrap.reset();
    }

    @AfterEach
    void destroy() {
        DubboBootstrap.reset();
    }

    @Test
    void testMultiConfig() {
        // application
        assertEquals("dubbo-demo-multi-application", application.getName());
        // module
        assertEquals("dubbo-demo-module", module.getName());
        // registry
        assertEquals("test://192.168.99.100:32770", registry.getAddress());
        assertEquals("test", registry.getProtocol());
        assertEquals(Integer.valueOf(32770), registry.getPort());
        // monitor
        assertEquals("test://127.0.0.1:32770", monitor.getAddress());
        // protocol
        assertEquals("dubbo", protocol.getName());
        assertEquals(Integer.valueOf(20880), protocol.getPort());
        // consumer
        assertEquals("netty", consumer.getClient());
        // provider
        assertEquals("127.0.0.1", provider.getHost());
    }
}
