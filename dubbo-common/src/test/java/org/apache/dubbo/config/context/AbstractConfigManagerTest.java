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

import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigManagerTest.TestPreferSerializationProvider;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractConfigManagerTest {

    private ConfigManager configManager;
    private ModuleConfigManager moduleConfigManager;

    @BeforeEach
    public void init() {
        ApplicationModel.defaultModel().destroy();
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        configManager = applicationModel.getApplicationConfigManager();
        moduleConfigManager = applicationModel.getDefaultModule().getConfigManager();
        FrameworkModel.defaultModel().getBeanFactory().registerBean(TestPreferSerializationProvider.class);
    }

    @Test
    void testRegistryConfigSameAddressDifferentId() {
        RegistryConfig zk1 = new RegistryConfig();
        zk1.setId("zk1");
        zk1.setProtocol("zookeeper");
        zk1.setAddress("10.47.181.23:2181,10.47.181.24:2181,10.47.181.25:2181");

        RegistryConfig zk2 = new RegistryConfig();
        zk2.setId("zk2");
        zk2.setProtocol("zookeeper");
        zk2.setAddress("10.47.181.23:2181,10.47.181.24:2181,10.47.181.25:2181");

        configManager.addConfig(zk1);
        configManager.addConfig(zk2);

        assertTrue(configManager.getConfig(RegistryConfig.class, "zk1").isPresent());
        assertTrue(configManager.getConfig(RegistryConfig.class, "zk2").isPresent());
    }
}
