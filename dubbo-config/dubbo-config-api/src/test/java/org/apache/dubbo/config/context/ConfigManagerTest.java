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
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.config.context.ConfigManager.getInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ConfigManager} Test
 *
 * @since 2.7.4
 */
public class ConfigManagerTest {

    private ConfigManager configManager = getInstance();

    @BeforeEach
    public void init() {
        configManager.clear();
        assertFalse(configManager.getApplication().isPresent());
        assertFalse(configManager.getMonitor().isPresent());
        assertFalse(configManager.getMonitor().isPresent());
    }

    @Test
    public void testApplicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        configManager.setApplication(applicationConfig);
        assertTrue(configManager.getApplication().isPresent());
        assertEquals(applicationConfig, configManager.getApplication().get());
    }

    @Test
    public void testMonitorConfig() {
        MonitorConfig monitorConfig = new MonitorConfig();
        configManager.setMonitor(monitorConfig);
        assertTrue(configManager.getMonitor().isPresent());
        assertEquals(monitorConfig, configManager.getMonitor().get());
    }

    @Test
    public void tesModuleConfig() {
        ModuleConfig config = new ModuleConfig();
        configManager.setModule(config);
        assertTrue(configManager.getModule().isPresent());
        assertEquals(config, configManager.getModule().get());
    }
}
