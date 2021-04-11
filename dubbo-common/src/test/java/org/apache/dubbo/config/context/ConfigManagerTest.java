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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.rpc.model.ApplicationModel.getConfigManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ConfigManager} Test
 *
 * @since 2.7.5
 */
public class ConfigManagerTest {

    private ConfigManager configManager = getConfigManager();

    @BeforeEach
    public void init() {
        configManager.destroy();
    }

    @Test
    public void testDestroy() {
        assertTrue(configManager.configsCache.isEmpty());
    }

    @Test
    public void testDefaultValues() {
        // assert single
        assertFalse(configManager.getApplication().isPresent());
        assertFalse(configManager.getMonitor().isPresent());
        assertFalse(configManager.getModule().isPresent());
        assertFalse(configManager.getMetrics().isPresent());

        // providers and consumers
        assertFalse(configManager.getDefaultProvider().isPresent());
        assertFalse(configManager.getDefaultConsumer().isPresent());
        assertTrue(configManager.getProviders().isEmpty());
        assertTrue(configManager.getConsumers().isEmpty());

        // protocols
        assertTrue(configManager.getProtocols().isEmpty());
        assertTrue(configManager.getDefaultProtocols().isEmpty());
        assertTrue(configManager.getProtocolIds().isEmpty());

        // registries
        assertTrue(configManager.getRegistries().isEmpty());
        assertTrue(configManager.getDefaultRegistries().isEmpty());
        assertTrue(configManager.getRegistryIds().isEmpty());

        // services and references
        assertTrue(configManager.getServices().isEmpty());
        assertTrue(configManager.getReferences().isEmpty());

        // config centers
        assertTrue(configManager.getConfigCenters().isEmpty());

        // metadata
        assertTrue(configManager.getMetadataConfigs().isEmpty());
    }

    // Test ApplicationConfig correlative methods
    @Test
    public void testApplicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        configManager.setApplication(config);
        assertTrue(configManager.getApplication().isPresent());
        assertEquals(config, configManager.getApplication().get());
    }

    // Test MonitorConfig correlative methods
    @Test
    public void testMonitorConfig() {
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setGroup("test");
        configManager.setMonitor(monitorConfig);
        assertTrue(configManager.getMonitor().isPresent());
        assertEquals(monitorConfig, configManager.getMonitor().get());
    }

    // Test MonitorConfig correlative methods
    @Test
    public void tesModuleConfig() {
        ModuleConfig config = new ModuleConfig();
        configManager.setModule(config);
        assertTrue(configManager.getModule().isPresent());
        assertEquals(config, configManager.getModule().get());
    }

    // Test MetricsConfig correlative methods
    @Test
    public void tesMetricsConfig() {
        MetricsConfig config = new MetricsConfig();
        configManager.setMetrics(config);
        assertTrue(configManager.getMetrics().isPresent());
        assertEquals(config, configManager.getMetrics().get());
    }

    // Test ProviderConfig correlative methods
    @Test
    public void testProviderConfig() {
        ProviderConfig config = new ProviderConfig();
        configManager.addProviders(asList(config, null));
        Collection<ProviderConfig> configs = configManager.getProviders();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertTrue(configManager.getDefaultProvider().isPresent());

        config.setId(DEFAULT_KEY);
        configManager.addProvider(config);
        assertTrue(configManager.getDefaultProvider().isPresent());
        configs = configManager.getProviders();
        assertEquals(2, configs.size());
    }

    // Test ConsumerConfig correlative methods
    @Test
    public void testConsumerConfig() {
        ConsumerConfig config = new ConsumerConfig();
        configManager.addConsumers(asList(config, null));
        Collection<ConsumerConfig> configs = configManager.getConsumers();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertTrue(configManager.getDefaultConsumer().isPresent());

        config.setId(DEFAULT_KEY);
        configManager.addConsumer(config);
        assertTrue(configManager.getDefaultConsumer().isPresent());
        configs = configManager.getConsumers();
        assertEquals(2, configs.size());
    }

    // Test ProtocolConfig correlative methods
    @Test
    public void testProtocolConfig() {
        ProtocolConfig config = new ProtocolConfig();
        configManager.addProtocols(asList(config, null));
        Collection<ProtocolConfig> configs = configManager.getProtocols();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertFalse(configManager.getDefaultProtocols().isEmpty());
    }

    // Test RegistryConfig correlative methods
    @Test
    public void testRegistryConfig() {
        RegistryConfig config = new RegistryConfig();
        configManager.addRegistries(asList(config, null));
        Collection<RegistryConfig> configs = configManager.getRegistries();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertFalse(configManager.getDefaultRegistries().isEmpty());
    }

    // Test ConfigCenterConfig correlative methods
    @Test
    public void testConfigCenterConfig() {
        ConfigCenterConfig config = new ConfigCenterConfig();
        configManager.addConfigCenters(asList(config, null));
        Collection<ConfigCenterConfig> configs = configManager.getConfigCenters();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
    }

    @Test
    public void testAddConfig() {
        configManager.addConfig(new ApplicationConfig());
        configManager.addConfig(new ProviderConfig());
        configManager.addConfig(new ProtocolConfig());

        assertTrue(configManager.getApplication().isPresent());
        assertFalse(configManager.getProviders().isEmpty());
        assertFalse(configManager.getProtocols().isEmpty());
    }

    @Test
    public void testRefreshAll() {
        configManager.refreshAll();
    }

    @Test
    public void testDefaultConfig() {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setDefault(false);
        assertFalse(ConfigManager.isDefaultConfig(providerConfig));

        ProviderConfig providerConfig1 = new ProviderConfig();
        assertTrue(ConfigManager.isDefaultConfig(providerConfig1));

        ProviderConfig providerConfig3 = new ProviderConfig();
        providerConfig.setDefault(true);
        assertTrue(ConfigManager.isDefaultConfig(providerConfig3));

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setDefault(false);
        assertFalse(ConfigManager.isDefaultConfig(protocolConfig));
    }
}
