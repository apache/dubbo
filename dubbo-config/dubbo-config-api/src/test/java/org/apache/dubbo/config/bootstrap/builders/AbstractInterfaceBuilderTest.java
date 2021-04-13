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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class AbstractInterfaceBuilderTest {

    @Test
    void local() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.local("GreetingMock");
        Assertions.assertEquals("GreetingMock", builder.build().getLocal());
    }

    @Test
    void local1() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.local((Boolean) null);
        Assertions.assertNull(builder.build().getLocal());
        builder.local(false);
        Assertions.assertEquals("false", builder.build().getLocal());
        builder.local(true);
        Assertions.assertEquals("true", builder.build().getLocal());
    }

    @Test
    void stub() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.stub("GreetingMock");
        Assertions.assertEquals("GreetingMock", builder.build().getStub());
    }

    @Test
    void stub1() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.stub((Boolean) null);
        Assertions.assertNull(builder.build().getLocal());
        builder.stub(false);
        Assertions.assertEquals("false", builder.build().getStub());
        builder.stub(true);
        Assertions.assertEquals("true", builder.build().getStub());
    }

    @Test
    void monitor() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.monitor("123");

        MonitorConfig monitorConfig = new MonitorConfig("123");
        Assertions.assertEquals(monitorConfig, builder.build().getMonitor());
    }

    @Test
    void monitor1() {
        MonitorConfig monitorConfig = new MonitorConfig("123");
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.monitor(monitorConfig);

        Assertions.assertEquals(monitorConfig, builder.build().getMonitor());
    }

    @Test
    void proxy() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.proxy("mockproxyfactory");

        Assertions.assertEquals("mockproxyfactory", builder.build().getProxy());
    }

    @Test
    void cluster() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.cluster("mockcluster");

        Assertions.assertEquals("mockcluster", builder.build().getCluster());
    }

    @Test
    void filter() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.filter("mockfilter");

        Assertions.assertEquals("mockfilter", builder.build().getFilter());
    }

    @Test
    void listener() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.listener("mockinvokerlistener");

        Assertions.assertEquals("mockinvokerlistener", builder.build().getListener());
    }

    @Test
    void owner() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.owner("owner");

        Assertions.assertEquals("owner", builder.build().getOwner());
    }

    @Test
    void connections() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.connections(1);

        Assertions.assertEquals(1, builder.build().getConnections().intValue());
    }

    @Test
    void layer() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.layer("layer");

        Assertions.assertEquals("layer", builder.build().getLayer());
    }

    @Test
    void application() {
        ApplicationConfig applicationConfig = new ApplicationConfig();

        InterfaceBuilder builder = new InterfaceBuilder();
        builder.application(applicationConfig);

        Assertions.assertEquals(applicationConfig, builder.build().getApplication());
    }

    @Test
    void module() {
        ModuleConfig moduleConfig = new ModuleConfig();
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.module(moduleConfig);

        Assertions.assertEquals(moduleConfig, builder.build().getModule());
    }

    @Test
    void addRegistries() {
        RegistryConfig registryConfig = new RegistryConfig();

        InterfaceBuilder builder = new InterfaceBuilder();
        builder.addRegistries(Collections.singletonList(registryConfig));

        Assertions.assertEquals(1, builder.build().getRegistries().size());
        Assertions.assertSame(registryConfig, builder.build().getRegistries().get(0));
        Assertions.assertSame(registryConfig, builder.build().getRegistry());
    }

    @Test
    void addRegistry() {
        RegistryConfig registryConfig = new RegistryConfig();

        InterfaceBuilder builder = new InterfaceBuilder();
        builder.addRegistry(registryConfig);

        Assertions.assertEquals(1, builder.build().getRegistries().size());
        Assertions.assertSame(registryConfig, builder.build().getRegistries().get(0));
        Assertions.assertSame(registryConfig, builder.build().getRegistry());
    }

    @Test
    void registryIds() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.registryIds("registryIds");

        Assertions.assertEquals("registryIds", builder.build().getRegistryIds());
    }

    @Test
    void onconnect() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.onconnect("onconnect");

        Assertions.assertEquals("onconnect", builder.build().getOnconnect());
    }

    @Test
    void ondisconnect() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.ondisconnect("ondisconnect");

        Assertions.assertEquals("ondisconnect", builder.build().getOndisconnect());
    }

    @Test
    void metadataReportConfig() {
        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();

        InterfaceBuilder builder = new InterfaceBuilder();
        builder.metadataReportConfig(metadataReportConfig);

        Assertions.assertEquals(metadataReportConfig, builder.build().getMetadataReportConfig());
    }

    @Test
    void configCenter() {
        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();

        InterfaceBuilder builder = new InterfaceBuilder();
        builder.configCenter(configCenterConfig);

        Assertions.assertEquals(configCenterConfig, builder.build().getConfigCenter());
    }

    @Test
    void callbacks() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.callbacks(2);
        Assertions.assertEquals(2, builder.build().getCallbacks().intValue());
    }

    @Test
    void scope() {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.scope("scope");

        Assertions.assertEquals("scope", builder.build().getScope());
    }

    @Test
    void build() {
        MonitorConfig monitorConfig = new MonitorConfig("123");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        ModuleConfig moduleConfig = new ModuleConfig();
        RegistryConfig registryConfig = new RegistryConfig();
        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();

        InterfaceBuilder builder = new InterfaceBuilder();
        builder.id("id").prefix("prefix").local(true).stub(false).monitor("123").proxy("mockproxyfactory").cluster("mockcluster")
                .filter("mockfilter").listener("mockinvokerlistener").owner("owner").connections(1)
                .layer("layer").application(applicationConfig).module(moduleConfig)
                .addRegistry(registryConfig).registryIds("registryIds")
                .onconnect("onconnet").ondisconnect("ondisconnect")
                .metadataReportConfig(metadataReportConfig)
                .configCenter(configCenterConfig)
                .callbacks(2).scope("scope");

        InterfaceConfig config = builder.build();
        InterfaceConfig config2 = builder.build();

        Assertions.assertEquals("id", config.getId());
        Assertions.assertEquals("prefix", config.getPrefix());
        Assertions.assertEquals("true", config.getLocal());
        Assertions.assertEquals("false", config.getStub());
        Assertions.assertEquals(monitorConfig, config.getMonitor());
        Assertions.assertEquals("mockproxyfactory", config.getProxy());
        Assertions.assertEquals("mockcluster", config.getCluster());
        Assertions.assertEquals("mockfilter", config.getFilter());
        Assertions.assertEquals("mockinvokerlistener", config.getListener());
        Assertions.assertEquals("owner", config.getOwner());
        Assertions.assertEquals(1, config.getConnections().intValue());
        Assertions.assertEquals("layer", config.getLayer());
        Assertions.assertEquals(applicationConfig, config.getApplication());
        Assertions.assertEquals(moduleConfig, config.getModule());
        Assertions.assertEquals(registryConfig, config.getRegistry());
        Assertions.assertEquals("registryIds", config.getRegistryIds());
        Assertions.assertEquals("onconnet", config.getOnconnect());
        Assertions.assertEquals("ondisconnect", config.getOndisconnect());
        Assertions.assertEquals(metadataReportConfig, config.getMetadataReportConfig());
        Assertions.assertEquals(configCenterConfig, config.getConfigCenter());
        Assertions.assertEquals(2, config.getCallbacks().intValue());
        Assertions.assertEquals("scope", config.getScope());

        Assertions.assertNotSame(config, config2);
    }

    private static class InterfaceBuilder extends AbstractInterfaceBuilder<InterfaceConfig, InterfaceBuilder> {

        public InterfaceConfig build() {
            InterfaceConfig config = new InterfaceConfig();
            super.build(config);

            return config;
        }

        @Override
        protected InterfaceBuilder getThis() {
            return this;
        }
    }

    private static class InterfaceConfig extends AbstractInterfaceConfig {
    }
}