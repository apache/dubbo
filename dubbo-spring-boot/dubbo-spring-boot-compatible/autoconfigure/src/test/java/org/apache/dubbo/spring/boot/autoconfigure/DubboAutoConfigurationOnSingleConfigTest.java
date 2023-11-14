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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link DubboAutoConfiguration} Test On single Dubbo Configuration
 *
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(
        properties = {
            "dubbo.application.name = dubbo-demo-single-application",
            "dubbo.module.name = dubbo-demo-module",
            "dubbo.registry.address = test://192.168.99.100:32770",
            "dubbo.protocol.name=dubbo",
            "dubbo.protocol.port=20880",
            "dubbo.monitor.address=test://127.0.0.1:32770",
            "dubbo.provider.host=127.0.0.1",
            "dubbo.consumer.client=netty"
        })
@SpringBootTest(classes = {DubboAutoConfigurationOnSingleConfigTest.class})
@EnableAutoConfiguration
@ComponentScan
public class DubboAutoConfigurationOnSingleConfigTest {

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private ModuleConfig moduleConfig;

    @Autowired
    private RegistryConfig registryConfig;

    @Autowired
    private MonitorConfig monitorConfig;

    @Autowired
    private ProviderConfig providerConfig;

    @Autowired
    private ConsumerConfig consumerConfig;

    @Autowired
    private ProtocolConfig protocolConfig;

    @Before
    public void init() {
        DubboBootstrap.reset();
    }

    @After
    public void destroy() {
        DubboBootstrap.reset();
    }

    @Test
    public void testSingleConfig() {
        // application
        Assert.assertEquals("dubbo-demo-single-application", applicationConfig.getName());
        // module
        Assert.assertEquals("dubbo-demo-module", moduleConfig.getName());
        // registry
        Assert.assertEquals("test://192.168.99.100:32770", registryConfig.getAddress());
        // monitor
        Assert.assertEquals("test://127.0.0.1:32770", monitorConfig.getAddress());
        // protocol
        Assert.assertEquals("dubbo", protocolConfig.getName());
        Assert.assertEquals(Integer.valueOf(20880), protocolConfig.getPort());
        // consumer
        Assert.assertEquals("netty", consumerConfig.getClient());
        // provider
        Assert.assertEquals("127.0.0.1", providerConfig.getHost());
    }
}
