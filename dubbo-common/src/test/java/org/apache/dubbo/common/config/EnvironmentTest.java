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
package org.apache.dubbo.common.config;

import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.wrapper.CompositeDynamicConfiguration;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Environment}
 */
public class EnvironmentTest {

    @Test
    public void testResolvePlaceholders() {
        Environment environment = ApplicationModel.defaultModel().getModelEnvironment();

        Map<String, String> externalMap = new LinkedHashMap<>();
        externalMap.put("zookeeper.address", "127.0.0.1");
        externalMap.put("zookeeper.port", "2181");
        environment.updateAppExternalConfigMap(externalMap);

        Map<String, String> sysprops = new LinkedHashMap<>();
        sysprops.put("zookeeper.address", "192.168.10.1");
        System.getProperties().putAll(sysprops);

        try {
            String s = environment.resolvePlaceholders("zookeeper://${zookeeper.address}:${zookeeper.port}");
            assertEquals("zookeeper://192.168.10.1:2181", s);
        } finally {
            for (String key : sysprops.keySet()) {
                System.clearProperty(key);
            }
        }

    }

    @Test
    public void test() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);
        Environment environment = applicationModel.getModelEnvironment();

        // test getPrefixedConfiguration
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("127.0.0.1");
        registryConfig.setPort(2181);
        String prefix = "dubbo.registry";
        Configuration prefixedConfiguration = environment.getPrefixedConfiguration(registryConfig, prefix);
        Assertions.assertTrue(prefixedConfiguration instanceof PrefixedConfiguration);

        // test getConfigurationMaps(AbstractConfig config, String prefix)
        List<Map<String, String>> configurationMaps = environment.getConfigurationMaps(registryConfig, prefix);
        Assertions.assertEquals(configurationMaps.size(), 7);

        // test getConfigurationMaps()
        configurationMaps = environment.getConfigurationMaps();
        Assertions.assertEquals(configurationMaps.size(), 6);

        CompositeConfiguration configuration1 = environment.getConfiguration();
        CompositeConfiguration configuration2 = environment.getConfiguration();
        Assertions.assertEquals(configuration1, configuration2);

        // test getDynamicConfiguration
        Optional<DynamicConfiguration> dynamicConfiguration = environment.getDynamicConfiguration();
        Assertions.assertFalse(dynamicConfiguration.isPresent());
        // test getDynamicGlobalConfiguration
        Configuration dynamicGlobalConfiguration = environment.getDynamicGlobalConfiguration();
        Assertions.assertEquals(dynamicGlobalConfiguration, configuration1);

        CompositeDynamicConfiguration compositeDynamicConfiguration = new CompositeDynamicConfiguration();
        environment.setDynamicConfiguration(compositeDynamicConfiguration);
        dynamicConfiguration = environment.getDynamicConfiguration();
        Assertions.assertEquals(dynamicConfiguration.get(), compositeDynamicConfiguration);

        dynamicGlobalConfiguration = environment.getDynamicGlobalConfiguration();
        Assertions.assertNotEquals(dynamicGlobalConfiguration, configuration1);

        // test destroy
        environment.destroy();
        Assertions.assertNull(environment.getSystemConfiguration());
        Assertions.assertNull(environment.getEnvironmentConfiguration());
        Assertions.assertNull(environment.getAppExternalConfiguration());
        Assertions.assertNull(environment.getExternalConfiguration());
        Assertions.assertNull(environment.getAppConfiguration());

        frameworkModel.destroy();
    }
}
