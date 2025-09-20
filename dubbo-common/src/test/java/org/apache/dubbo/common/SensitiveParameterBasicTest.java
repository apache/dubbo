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
package org.apache.dubbo.common;

import org.apache.dubbo.common.utils.SensitiveParameterUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test to verify sensitive parameter functionality works correctly
 * after the refactoring to separate configuration from utility methods.
 */
public class SensitiveParameterBasicTest {

    @Test
    void testDefaultSensitiveParameters() {
        // Test that default sensitive parameters are recognized
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("accessKey"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("secretKey"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("password"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("token"));

        // Test that non-sensitive parameters return false
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("host"));
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("port"));
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("timeout"));
    }

    @Test
    void testApplicationConfigSensitiveParameters() {
        // Test that ApplicationConfig can be configured with sensitive parameters
        ApplicationConfig config = new ApplicationConfig();
        config.setAdditionalSensitiveParameters("customSecret,myToken");

        assertEquals("customSecret,myToken", config.getAdditionalSensitiveParameters());
    }

    @Test
    void testConfigManagerIntegration() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        // Create and add configuration
        ApplicationConfig config = new ApplicationConfig();
        config.setAdditionalSensitiveParameters("customParam");

        applicationModel.getConfigManager().setApplication(config);

        // Verify configuration is stored
        assertNotNull(applicationModel.getConfigManager().getApplication().orElse(null));
        assertEquals(
                "customParam",
                applicationModel.getConfigManager().getApplication().get().getAdditionalSensitiveParameters());
    }
}
