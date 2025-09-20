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

import org.apache.dubbo.common.utils.SensitiveParameterUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerSensitiveParameterTest {

    private ApplicationModel applicationModel;
    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        applicationModel = ApplicationModel.defaultModel();
        configManager = applicationModel.getApplicationConfigManager();
    }

    @AfterEach
    void tearDown() {
        // Reset ApplicationConfig if exists
        Optional<ApplicationConfig> config = configManager.getApplication();
        if (config.isPresent()) {
            config.get().setAdditionalSensitiveParameters(null);
        }
    }

    @Test
    void testAddApplicationConfigWithSensitiveParameters() {
        // Get existing ApplicationConfig and configure it
        ApplicationConfig applicationConfig = configManager.getApplication().get();
        applicationConfig.setAdditionalSensitiveParameters("customParam1,customParam2");

        // Verify it was configured
        Optional<ApplicationConfig> retrievedConfig = configManager.getApplication();
        assertTrue(retrievedConfig.isPresent());
        assertEquals("customParam1,customParam2", retrievedConfig.get().getAdditionalSensitiveParameters());
    }

    @Test
    void testSensitiveParameterUtilsIntegration() {
        // Get existing ApplicationConfig and configure it
        ApplicationConfig config = configManager.getApplication().get();
        config.setAdditionalSensitiveParameters("apiKey,authToken");

        // Verify the utility methods work with the configured parameters
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("apiKey"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("authToken"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("username")); // default parameter

        // Verify non-sensitive parameters
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("timeout"));
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("host"));
    }

    @Test
    void testSensitiveParameterUtilsWithoutApplicationConfig() {
        // Test behavior when no ApplicationConfig is set
        // Should still work with default parameters
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("password"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("accessKey"));
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("timeout"));
    }
}
