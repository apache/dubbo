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
package org.apache.dubbo.config.context;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;
import static org.apache.dubbo.config.context.ConfigManager.DUBBO_CONFIG_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link AbstractConfigManager} Test
 * {@link ConfigManager} Test
 * {@link ModuleConfigManager} Test
 *
 * @since 2.7.5
 */
class ConfigManagerTest {

    private ConfigManager configManager;
    private ModuleConfigManager moduleConfigManager;

    @BeforeEach
    public void init() {
        ApplicationModel.defaultModel().destroy();
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        configManager = applicationModel.getApplicationConfigManager();
        moduleConfigManager = applicationModel.getDefaultModule().getConfigManager();
    }

    @Test
    void testDestroy() {
        assertTrue(configManager.configsCache.isEmpty());
    }

    @Test
    void testDefaultValues() {
        // assert single
        assertFalse(configManager.getApplication().isPresent());
        assertFalse(configManager.getMonitor().isPresent());
        assertFalse(configManager.getMetrics().isPresent());

        // protocols
        assertTrue(configManager.getProtocols().isEmpty());
        assertTrue(configManager.getDefaultProtocols().isEmpty());

        // registries
        assertTrue(configManager.getRegistries().isEmpty());
        assertTrue(configManager.getDefaultRegistries().isEmpty());

        // config centers
        assertTrue(configManager.getConfigCenters().isEmpty());

        // metadata
        assertTrue(configManager.getMetadataConfigs().isEmpty());

        // services and references
        assertTrue(moduleConfigManager.getServices().isEmpty());
        assertTrue(moduleConfigManager.getReferences().isEmpty());

        // providers and consumers
        assertFalse(moduleConfigManager.getModule().isPresent());
        assertFalse(moduleConfigManager.getDefaultProvider().isPresent());
        assertFalse(moduleConfigManager.getDefaultConsumer().isPresent());
        assertTrue(moduleConfigManager.getProviders().isEmpty());
        assertTrue(moduleConfigManager.getConsumers().isEmpty());
    }

    // Test ApplicationConfig correlative methods
    @Test
    void testApplicationConfig() {
        ApplicationConfig config = new ApplicationConfig("ConfigManagerTest");
        configManager.setApplication(config);
        assertTrue(configManager.getApplication().isPresent());
        assertEquals(config, configManager.getApplication().get());
        assertEquals(config, moduleConfigManager.getApplication().get());
    }

    // Test MonitorConfig correlative methods
    @Test
    void testMonitorConfig() {
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setGroup("test");
        configManager.setMonitor(monitorConfig);
        assertTrue(configManager.getMonitor().isPresent());
        assertEquals(monitorConfig, configManager.getMonitor().get());
        assertEquals(monitorConfig, moduleConfigManager.getMonitor().get());
    }

    // Test ModuleConfig correlative methods
    @Test
    void testModuleConfig() {
        ModuleConfig config = new ModuleConfig();
        moduleConfigManager.setModule(config);
        assertTrue(moduleConfigManager.getModule().isPresent());
        assertEquals(config, moduleConfigManager.getModule().get());
    }

    // Test MetricsConfig correlative methods
    @Test
    void testMetricsConfig() {
        MetricsConfig config = new MetricsConfig();
        config.setProtocol(PROTOCOL_PROMETHEUS);
        configManager.setMetrics(config);
        assertTrue(configManager.getMetrics().isPresent());
        assertEquals(config, configManager.getMetrics().get());
        assertEquals(config, moduleConfigManager.getMetrics().get());
    }

    // Test ProviderConfig correlative methods
    @Test
    void testProviderConfig() {
        ProviderConfig config = new ProviderConfig();
        moduleConfigManager.addProviders(asList(config, null));
        Collection<ProviderConfig> configs = moduleConfigManager.getProviders();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertTrue(moduleConfigManager.getDefaultProvider().isPresent());

        config = new ProviderConfig();
        config.setId(DEFAULT_KEY);
        config.setQueues(10);
        moduleConfigManager.addProvider(config);
        assertTrue(moduleConfigManager.getDefaultProvider().isPresent());
        configs = moduleConfigManager.getProviders();
        assertEquals(2, configs.size());
    }

    // Test ConsumerConfig correlative methods
    @Test
    void testConsumerConfig() {
        ConsumerConfig config = new ConsumerConfig();
        moduleConfigManager.addConsumers(asList(config, null));
        Collection<ConsumerConfig> configs = moduleConfigManager.getConsumers();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertTrue(moduleConfigManager.getDefaultConsumer().isPresent());

        config = new ConsumerConfig();
        config.setId(DEFAULT_KEY);
        config.setThreads(10);
        moduleConfigManager.addConsumer(config);
        assertTrue(moduleConfigManager.getDefaultConsumer().isPresent());
        configs = moduleConfigManager.getConsumers();
        assertEquals(2, configs.size());
    }

    // Test ProtocolConfig correlative methods
    @Test
    void testProtocolConfig() {
        ProtocolConfig config = new ProtocolConfig();
        configManager.addProtocols(asList(config, null));
        Collection<ProtocolConfig> configs = configManager.getProtocols();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertFalse(configManager.getDefaultProtocols().isEmpty());
        assertEquals(configs, moduleConfigManager.getProtocols());
        assertNotEquals(20881, config.getPort());
        assertNotEquals(config.getSerialization(),"fastjson2");
        ProtocolConfig defaultConfig = new ProtocolConfig();
        defaultConfig.setPort(20881);
        defaultConfig.setSerialization("fastjson2");
        config.mergeProtocol(defaultConfig);
        assertEquals(config.getPort(),20881);
        assertEquals(config.getSerialization(),"fastjson2");
    }

    // Test RegistryConfig correlative methods
    @Test
    void testRegistryConfig() {
        RegistryConfig config = new RegistryConfig();
        configManager.addRegistries(asList(config, null));
        Collection<RegistryConfig> configs = configManager.getRegistries();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertFalse(configManager.getDefaultRegistries().isEmpty());
        assertEquals(configs, moduleConfigManager.getRegistries());
    }

    // Test ConfigCenterConfig correlative methods
    @Test
    void testConfigCenterConfig() {
        String address = "zookeeper://127.0.0.1:2181";
        ConfigCenterConfig config = new ConfigCenterConfig();
        config.setAddress(address);
        configManager.addConfigCenters(asList(config, null));
        Collection<ConfigCenterConfig> configs = configManager.getConfigCenters();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());

        // add duplicated config, expecting ignore equivalent configs
        ConfigCenterConfig config2 = new ConfigCenterConfig();
        config2.setAddress(address);
        configManager.addConfigCenter(config2);

        configs = configManager.getConfigCenters();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertEquals(configs, moduleConfigManager.getConfigCenters());
    }

    @Test
    void testAddConfig() {
        configManager.addConfig(new ApplicationConfig("ConfigManagerTest"));
        configManager.addConfig(new ProtocolConfig());
        moduleConfigManager.addConfig(new ProviderConfig());

        assertTrue(configManager.getApplication().isPresent());
        assertFalse(configManager.getProtocols().isEmpty());
        assertFalse(moduleConfigManager.getProviders().isEmpty());
    }

    @Test
    void testRefreshAll() {
        configManager.refreshAll();
        moduleConfigManager.refreshAll();
    }

    @Test
    void testDefaultConfig() {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setDefault(false);
        assertFalse(ConfigManager.isDefaultConfig(providerConfig));

        ProviderConfig providerConfig1 = new ProviderConfig();
        assertNull(ConfigManager.isDefaultConfig(providerConfig1));

        ProviderConfig providerConfig3 = new ProviderConfig();
        providerConfig3.setDefault(true);
        assertTrue(ConfigManager.isDefaultConfig(providerConfig3));

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setDefault(false);
        assertFalse(ConfigManager.isDefaultConfig(protocolConfig));
    }

    @Test
    void testConfigMode() {
        ApplicationConfig applicationConfig1 = new ApplicationConfig("app1");
        ApplicationConfig applicationConfig2 = new ApplicationConfig("app2");

        try {
            // test strict mode
            ApplicationModel.reset();
            ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.STRICT, configManager.getConfigMode());

            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.STRICT.name());
            ApplicationModel.reset();
            Assertions.assertEquals(ConfigMode.STRICT, configManager.getConfigMode());

            configManager.addConfig(applicationConfig1);
            try {
                configManager.addConfig(applicationConfig2);
                fail("strict mode cannot add two application configs");
            } catch (Exception e) {
                assertEquals(IllegalStateException.class, e.getClass());
                assertTrue(e.getMessage().contains("please remove redundant configs and keep only one"));
            }

            // test override mode
            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.OVERRIDE.name());
            ApplicationModel.reset();
            configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.OVERRIDE, configManager.getConfigMode());

            configManager.addConfig(applicationConfig1);
            configManager.addConfig(applicationConfig2);
            assertEquals(applicationConfig2, configManager.getApplicationOrElseThrow());


            // test ignore mode
            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.IGNORE.name());
            ApplicationModel.reset();
            configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.IGNORE, configManager.getConfigMode());

            configManager.addConfig(applicationConfig1);
            configManager.addConfig(applicationConfig2);
            assertEquals(applicationConfig1, configManager.getApplicationOrElseThrow());

            // test OVERRIDE_ALL mode
            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.OVERRIDE_ALL.name());
            ApplicationModel.reset();
            configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.OVERRIDE_ALL, configManager.getConfigMode());

            ApplicationConfig applicationConfig11 = new ApplicationConfig("app11");
            ApplicationConfig applicationConfig22 = new ApplicationConfig("app22");
            applicationConfig11.setParameters(CollectionUtils.toStringMap("k1", "v1", "k2", "v2"));
            applicationConfig22.setParameters(CollectionUtils.toStringMap("k1", "v11", "k2", "v22", "k3", "v3"));
            configManager.addConfig(applicationConfig11);
            configManager.addConfig(applicationConfig22);

            assertEquals(applicationConfig11, configManager.getApplicationOrElseThrow());
            assertEquals(applicationConfig11.getName(), "app22");
            assertEquals(applicationConfig11.getParameters(), CollectionUtils.toStringMap("k1", "v11", "k2", "v22", "k3", "v3"));

            // test OVERRIDE_IF_ABSENT mode
            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.OVERRIDE_IF_ABSENT.name());
            ApplicationModel.reset();
            configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.OVERRIDE_IF_ABSENT, configManager.getConfigMode());

            ApplicationConfig applicationConfig33 = new ApplicationConfig("app33");
            ApplicationConfig applicationConfig44 = new ApplicationConfig("app44");
            applicationConfig33.setParameters(CollectionUtils.toStringMap("k1", "v1", "k2", "v2"));
            applicationConfig44.setParameters(CollectionUtils.toStringMap("k1", "v11", "k2", "v22", "k3", "v3"));
            configManager.addConfig(applicationConfig33);
            configManager.addConfig(applicationConfig44);

            assertEquals(applicationConfig33, configManager.getApplicationOrElseThrow());
            assertEquals("app33", applicationConfig33.getName());
            assertEquals(CollectionUtils.toStringMap("k1", "v1", "k2", "v2", "k3", "v3"), applicationConfig33.getParameters());
        } finally {
            System.clearProperty(DUBBO_CONFIG_MODE);
        }
    }

    @Test
    void testGetConfigByIdOrName() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setId("registryID_1");
        configManager.addRegistry(registryConfig);
        Optional<RegistryConfig> registryConfigOptional = configManager.getConfig(RegistryConfig.class, registryConfig.getId());

        if (registryConfigOptional.isPresent()) {
            Assertions.assertEquals(registryConfigOptional.get(), registryConfig);
        } else {
            fail("registryConfigOptional is empty! ");
        }

        ProtocolConfig protocolConfig = new ProtocolConfig("dubbo");
        configManager.addProtocol(protocolConfig);
        Optional<ProtocolConfig> protocolConfigOptional = configManager.getConfig(ProtocolConfig.class, protocolConfig.getName());

        if (protocolConfigOptional.isPresent()) {
            Assertions.assertEquals(protocolConfigOptional.get(), protocolConfig);
        } else {
            fail("protocolConfigOptional is empty! ");
        }

        // test multi config has same name(dubbo)
        ProtocolConfig protocolConfig2 = new ProtocolConfig("dubbo");
        protocolConfig2.setPort(9103);
        configManager.addProtocol(protocolConfig2);
        try {
            configManager.getConfig(ProtocolConfig.class, protocolConfig.getName());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalStateException);
            Assertions.assertEquals(e.getMessage(), "Found more than one config by name: dubbo, instances: " +
                "[<dubbo:protocol port=\"9103\" name=\"dubbo\" />, <dubbo:protocol name=\"dubbo\" />]. " +
                "Please remove redundant configs or get config by id.");
        }

        ModuleConfig moduleConfig = new ModuleConfig();
        moduleConfig.setId("moduleID_1");
        moduleConfigManager.setModule(moduleConfig);
        Optional<ModuleConfig> moduleConfigOptional = moduleConfigManager.getConfig(ModuleConfig.class, moduleConfig.getId());
        Assertions.assertEquals(moduleConfig, moduleConfigOptional.get());

        Optional<RegistryConfig> config = moduleConfigManager.getConfig(RegistryConfig.class, registryConfig.getId());
        Assertions.assertEquals(config.get(), registryConfig);
    }

    @Test
    void testLoadConfigsOfTypeFromProps() {
        try {
            // dubbo.application.enable-file-cache = false
            configManager.loadConfigsOfTypeFromProps(ApplicationConfig.class);
            Optional<ApplicationConfig> application = configManager.getApplication();
            Assertions.assertTrue(application.isPresent());
            configManager.removeConfig(application.get());

            System.setProperty("dubbo.protocols.dubbo1.port", "20880");
            System.setProperty("dubbo.protocols.dubbo2.port", "20881");
            System.setProperty("dubbo.protocols.rest1.port", "8080");
            System.setProperty("dubbo.protocols.rest2.port", "8081");
            configManager.loadConfigsOfTypeFromProps(ProtocolConfig.class);
            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(4, protocols.size());

            System.setProperty("dubbo.applications.app1.name", "app-demo1");
            System.setProperty("dubbo.applications.app2.name", "app-demo2");
            try {
                configManager.loadConfigsOfTypeFromProps(ApplicationConfig.class);
                Assertions.fail();
            } catch (Exception e) {
                Assertions.assertTrue(e.getMessage().contains("load config failed"));
            }
        } finally {
            System.clearProperty("dubbo.protocols.dubbo1.port");
            System.clearProperty("dubbo.protocols.dubbo2.port");
            System.clearProperty("dubbo.protocols.rest1.port");
            System.clearProperty("dubbo.protocols.rest2.port");
            System.clearProperty("dubbo.applications.app1.name");
            System.clearProperty("dubbo.applications.app2.name");
        }

    }

    @Test
    void testLoadConfig() {
        configManager.loadConfigs();
        Assertions.assertTrue(configManager.getApplication().isPresent());
        Assertions.assertTrue(configManager.getSsl().isPresent());
        Assertions.assertFalse(configManager.getProtocols().isEmpty());

        int port = 20880;
        ProtocolConfig config1 = new ProtocolConfig();
        config1.setName("dubbo");
        config1.setPort(port);

        ProtocolConfig config2 = new ProtocolConfig();
        config2.setName("rest");
        config2.setPort(port);
        configManager.addProtocols(asList(config1, config2));
        try {
            configManager.loadConfigs();
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalStateException);
            Assertions.assertTrue(e.getMessage().contains("Duplicated port used by protocol configs, port: " + port));
        }

        moduleConfigManager.loadConfigs();
        Assertions.assertTrue(moduleConfigManager.getModule().isPresent());
        Assertions.assertFalse(moduleConfigManager.getProviders().isEmpty());
        Assertions.assertFalse(moduleConfigManager.getConsumers().isEmpty());

    }
}
