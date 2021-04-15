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
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * {@link DubboAutoConfiguration} Test On multiple Dubbo Configuration
 *
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(
        properties = {
                "dubbo.applications.application1.NAME = dubbo-demo-application",
                "dubbo.modules.module1.name = dubbo-demo-module",
                "dubbo.registries.registry1.address = zookeeper://192.168.99.100:32770",
                "dubbo.protocols.protocol1.name=dubbo",
                "dubbo.protocols.protocol1.pORt=20880",
                "dubbo.monitors.monitor1.Address=zookeeper://127.0.0.1:32770",
                "dubbo.providers.provider1.host=127.0.0.1",
                "dubbo.consumers.consumer1.client=netty",
                "dubbo.config.multiple=true",
                "dubbo.scan.basePackages=org.apache.dubbo.spring.boot.dubbo, org.apache.dubbo.spring.boot.condition"
        }
)
@SpringBootTest(
        classes = {
                DubboAutoConfigurationOnMultipleConfigTest.class
        }
)
@EnableAutoConfiguration
public class DubboAutoConfigurationOnMultipleConfigTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * {@link ApplicationConfig}
     */
    @Autowired
    @Qualifier("application1")
    private ApplicationConfig application;

    /**
     * {@link ModuleConfig}
     */
    @Autowired
    @Qualifier("module1")
    private ModuleConfig module;

    /**
     * {@link RegistryConfig}
     */
    @Autowired
    @Qualifier("registry1")
    private RegistryConfig registry;

    /**
     * {@link ProtocolConfig}
     */
    @Autowired
    @Qualifier("protocol1")
    private ProtocolConfig protocol;

    /**
     * {@link MonitorConfig}
     */
    @Autowired
    @Qualifier("monitor1")
    private MonitorConfig monitor;

    /**
     * {@link ProviderConfig}
     */
    @Autowired
    @Qualifier("provider1")
    private ProviderConfig provider;

    /**
     * {@link ConsumerConfig}
     */
    @Autowired
    @Qualifier("consumer1")
    private ConsumerConfig consumer;

    @Before
    public void init() {
        ApplicationModel.reset();
    }

    @After
    public void destroy() {
        ApplicationModel.reset();
    }

    @Autowired
    private Map<String, ApplicationConfig> applications = new LinkedHashMap<>();

    @Autowired
    private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    @Autowired
    private Map<String, RegistryConfig> registries = new LinkedHashMap<>();

    @Autowired
    private Map<String, ProtocolConfig> protocols = new LinkedHashMap<>();

    @Autowired
    private Map<String, MonitorConfig> monitors = new LinkedHashMap<>();

    @Autowired
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    @Autowired
    private Map<String, ConsumerConfig> consumers = new LinkedHashMap<>();

    @Test
    public void testMultipleDubboConfigBindingProperties() {


        Assert.assertEquals(1, applications.size());

        Assert.assertEquals(1, modules.size());

        Assert.assertEquals(1, registries.size());

        Assert.assertEquals(1, protocols.size());

        Assert.assertEquals(1, monitors.size());

        Assert.assertEquals(1, providers.size());

        Assert.assertEquals(1, consumers.size());

    }

    @Test
    public void testApplicationContext() {

        /**
         * Multiple {@link ApplicationConfig}
         */
        Map<String, ApplicationConfig> applications = beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class);

        Assert.assertEquals(1, applications.size());

        /**
         * Multiple {@link ModuleConfig}
         */
        Map<String, ModuleConfig> modules = beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class);

        Assert.assertEquals(1, modules.size());

        /**
         * Multiple {@link RegistryConfig}
         */
        Map<String, RegistryConfig> registries = beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class);

        Assert.assertEquals(1, registries.size());

        /**
         * Multiple {@link ProtocolConfig}
         */
        Map<String, ProtocolConfig> protocols = beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class);

        Assert.assertEquals(1, protocols.size());

        /**
         * Multiple {@link MonitorConfig}
         */
        Map<String, MonitorConfig> monitors = beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class);

        Assert.assertEquals(1, monitors.size());

        /**
         * Multiple {@link ProviderConfig}
         */
        Map<String, ProviderConfig> providers = beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class);

        Assert.assertEquals(1, providers.size());

        /**
         * Multiple {@link ConsumerConfig}
         */
        Map<String, ConsumerConfig> consumers = beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class);

        Assert.assertEquals(1, consumers.size());

    }

    @Test
    public void testApplicationConfig() {

        Assert.assertEquals("dubbo-demo-application", application.getName());

    }

    @Test
    public void testModuleConfig() {

        Assert.assertEquals("dubbo-demo-module", module.getName());

    }

    @Test
    public void testRegistryConfig() {

        Assert.assertEquals("zookeeper://192.168.99.100:32770", registry.getAddress());

    }

    @Test
    public void testMonitorConfig() {

        Assert.assertEquals("zookeeper://127.0.0.1:32770", monitor.getAddress());

    }

    @Test
    public void testProtocolConfig() {

        Assert.assertEquals("dubbo", protocol.getName());
        Assert.assertEquals(Integer.valueOf(20880), protocol.getPort());

    }

    @Test
    public void testConsumerConfig() {

        Assert.assertEquals("netty", consumer.getClient());

    }
}
