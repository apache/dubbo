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

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveParameterConfigTest {

    @BeforeEach
    void setUp() {
        // Clear system properties and reset to defaults before each test
        System.clearProperty(SensitiveParameterConfig.SENSITIVE_PARAMS_PROPERTY);
        SensitiveParameterConfig.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        System.clearProperty(SensitiveParameterConfig.SENSITIVE_PARAMS_PROPERTY);
        SensitiveParameterConfig.resetToDefaults();
    }

    @Test
    void testDefaultSensitiveParameters() {
        // Test default sensitive parameters
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("password"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("accessKey"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("secretKey"));

        // Test non-sensitive parameters
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("host"));
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("port"));
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("timeout"));

        // Test null and empty
        assertFalse(SensitiveParameterConfig.isSensitiveParameter(null));
        assertFalse(SensitiveParameterConfig.isSensitiveParameter(""));
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("   "));
    }

    @Test
    void testGetDefaultSensitiveParameters() {
        Set<String> defaults = SensitiveParameterConfig.getDefaultSensitiveParameters();

        assertEquals(4, defaults.size());
        assertTrue(defaults.contains("username"));
        assertTrue(defaults.contains("password"));
        assertTrue(defaults.contains("accessKey"));
        assertTrue(defaults.contains("secretKey"));

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            defaults.add("newParam");
        });
    }

    @Test
    void testAddCustomSensitiveParameters() {
        // Initially no custom parameters
        assertEquals(0, SensitiveParameterConfig.getCustomSensitiveParameters().size());

        // Add custom sensitive parameters
        SensitiveParameterConfig.addSensitiveParameters("apiKey", "token");

        // Test they are now considered sensitive
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("apiKey"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("token"));

        // Default ones should still work
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("password"));

        // Test getAllSensitiveParameters includes both default and custom
        Set<String> allParams = SensitiveParameterConfig.getAllSensitiveParameters();
        assertEquals(6, allParams.size()); // 4 default + 2 custom
        assertTrue(allParams.contains("username"));
        assertTrue(allParams.contains("apiKey"));
        assertTrue(allParams.contains("token"));
    }

    @Test
    void testRemoveCustomSensitiveParameters() {
        // Add some custom parameters first
        SensitiveParameterConfig.addSensitiveParameters("apiKey", "token", "clientSecret");
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("apiKey"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("token"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("clientSecret"));

        // Remove some of them
        SensitiveParameterConfig.removeSensitiveParameters("apiKey", "nonExistent");

        // Check removal worked
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("apiKey"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("token"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("clientSecret"));

        // Default parameters should not be affected
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));
    }

    @Test
    void testSystemPropertyConfiguration() {
        // Set system property
        System.setProperty(
                SensitiveParameterConfig.SENSITIVE_PARAMS_PROPERTY, "customParam1,customParam2, customParam3 ");

        // Reset to reload configuration
        SensitiveParameterConfig.resetToDefaults();

        // Test custom parameters from system property are loaded
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("customParam1"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("customParam2"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("customParam3"));

        // Default parameters should still work
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));
    }

    @Test
    void testResetToDefaults() {
        // Add custom parameters
        SensitiveParameterConfig.addSensitiveParameters("customParam");
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("customParam"));

        // Reset to defaults
        SensitiveParameterConfig.resetToDefaults();

        // Custom parameter should no longer be sensitive
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("customParam"));

        // Default parameters should still work
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));
    }

    @Test
    void testCaching() {
        // Test that repeated calls are efficient (cached)
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));

        assertFalse(SensitiveParameterConfig.isSensitiveParameter("nonSensitive"));
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("nonSensitive"));

        // Add a custom parameter and verify cache is cleared
        SensitiveParameterConfig.addSensitiveParameters("newParam");
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("newParam"));
    }

    @Test
    void testEdgeCases() {
        // Test with null and empty parameters
        SensitiveParameterConfig.addSensitiveParameters((String[]) null);
        SensitiveParameterConfig.addSensitiveParameters();
        SensitiveParameterConfig.addSensitiveParameters("", "  ", null);

        // Should not affect existing functionality
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));

        // Test removing non-existent parameters
        SensitiveParameterConfig.removeSensitiveParameters("nonExistent");
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("username"));
    }

    @Test
    void testImmutability() {
        // Test that returned sets are immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            SensitiveParameterConfig.getDefaultSensitiveParameters().add("test");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            SensitiveParameterConfig.getCustomSensitiveParameters().add("test");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            SensitiveParameterConfig.getAllSensitiveParameters().add("test");
        });
    }

    @Test
    void testEnvironmentVariableConfiguration() {
        // This test simulates environment variable loading
        // Note: We can't actually set environment variables in unit tests,
        // but we can test the system property path which covers similar logic

        // Test with malformed configuration (empty values, spaces)
        System.setProperty(
                SensitiveParameterConfig.SENSITIVE_PARAMS_PROPERTY, "envParam1,,  envParam2  , , envParam3,");

        SensitiveParameterConfig.resetToDefaults();

        // Test that valid parameters are loaded and invalid ones are ignored
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("envParam1"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("envParam2"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("envParam3"));

        // Empty entries should be ignored
        assertFalse(SensitiveParameterConfig.isSensitiveParameter(""));
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("   "));
    }

    @Test
    void testConcurrentAccess() {
        // Test thread safety of lazy initialization
        SensitiveParameterConfig.resetToDefaults();

        // Simulate concurrent access to getCustomSensitiveParameters
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                SensitiveParameterConfig.getCustomSensitiveParameters();
                SensitiveParameterConfig.isSensitiveParameter("testParam");
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        assertDoesNotThrow(() -> {
            thread1.join(1000);
            thread2.join(1000);
        });
    }

    @Test
    void testParameterTrimming() {
        // Test that parameters are properly trimmed during add/remove operations
        SensitiveParameterConfig.addSensitiveParameters("  spacedParam  ", "\ttabbedParam\t", "\nnewlineParam\n");

        assertTrue(SensitiveParameterConfig.isSensitiveParameter("spacedParam"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("tabbedParam"));
        assertTrue(SensitiveParameterConfig.isSensitiveParameter("newlineParam"));

        // Original untrimmed versions should not be sensitive
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("  spacedParam  "));
        assertFalse(SensitiveParameterConfig.isSensitiveParameter("\ttabbedParam\t"));
    }
}
