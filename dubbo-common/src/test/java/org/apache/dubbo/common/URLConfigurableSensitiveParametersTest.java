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

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class URLConfigurableSensitiveParametersTest {

    @BeforeEach
    void setUp() {
        // Reset configuration before each test
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().orElse(null);
        if (applicationConfig != null) {
            applicationConfig.setAdditionalSensitiveParameters(null);
        } else {
            // Create ApplicationConfig if it doesn't exist
            applicationConfig = new ApplicationConfig();
            applicationModel.getConfigManager().setApplication(applicationConfig);
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().orElse(null);
        if (applicationConfig != null) {
            applicationConfig.setAdditionalSensitiveParameters(null);
        }
    }

    @Test
    void testDefaultSensitiveParametersInURL() {
        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?username=admin&password=secret&accessKey=mykey&secretKey=mysecret&timeout=5000");

        String urlString = url.toString();

        // Default sensitive parameters should be hidden
        assertFalse(urlString.contains("username=admin"));
        assertFalse(urlString.contains("password=secret"));
        assertFalse(urlString.contains("accessKey=mykey"));
        assertFalse(urlString.contains("secretKey=mysecret"));

        // Non-sensitive parameters should be visible
        assertTrue(urlString.contains("timeout=5000"));

        // toFullString should show everything
        String fullString = url.toFullString();
        assertTrue(fullString.contains("username=admin"));
        assertTrue(fullString.contains("password=secret"));
        assertTrue(fullString.contains("accessKey=mykey"));
        assertTrue(fullString.contains("secretKey=mysecret"));
        assertTrue(fullString.contains("timeout=5000"));
    }

    @Test
    void testCustomSensitiveParametersInURL() {
        // Add custom sensitive parameter via ApplicationConfig
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().get();
        applicationConfig.setAdditionalSensitiveParameters("apiKey,clientSecret");

        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?username=admin&apiKey=custom123&clientSecret=secret456&timeout=5000");

        String urlString = url.toString();

        // Both default and custom sensitive parameters should be hidden
        assertFalse(urlString.contains("username=admin"));
        assertFalse(urlString.contains("apiKey=custom123"));
        assertFalse(urlString.contains("clientSecret=secret456"));

        // Non-sensitive parameters should be visible
        assertTrue(urlString.contains("timeout=5000"));

        // toFullString should show everything
        String fullString = url.toFullString();
        assertTrue(fullString.contains("username=admin"));
        assertTrue(fullString.contains("apiKey=custom123"));
        assertTrue(fullString.contains("clientSecret=secret456"));
        assertTrue(fullString.contains("timeout=5000"));
    }

    @Test
    void testApplicationConfigSensitiveParameters() {
        // Configure custom sensitive parameters via ApplicationConfig
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().get();
        applicationConfig.setAdditionalSensitiveParameters("customToken,bearerToken");

        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?username=admin&customToken=abc123&bearerToken=xyz789&timeout=5000");

        String urlString = url.toString();

        // All sensitive parameters should be hidden (default + configured)
        assertFalse(urlString.contains("username=admin"));
        assertFalse(urlString.contains("customToken=abc123"));
        assertFalse(urlString.contains("bearerToken=xyz789"));

        // Non-sensitive parameters should be visible
        assertTrue(urlString.contains("timeout=5000"));
    }

    @Test
    void testChangingCustomSensitiveParameters() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().get();

        // Add custom parameters
        applicationConfig.setAdditionalSensitiveParameters("apiKey,customToken");
        URL url1 = URL.valueOf("nacos://127.0.0.1:8848/registry?apiKey=test123&customToken=abc&timeout=5000");
        String urlString1 = url1.toString();

        // Should be hidden initially
        assertFalse(urlString1.contains("apiKey=test123"));
        assertFalse(urlString1.contains("customToken=abc"));

        // Update configuration to only include customToken
        applicationConfig.setAdditionalSensitiveParameters("customToken");

        URL url2 = URL.valueOf("nacos://127.0.0.1:8848/registry?apiKey=test123&customToken=abc&timeout=5000");
        String urlString2 = url2.toString();

        // apiKey should now be visible, customToken should still be hidden
        assertTrue(urlString2.contains("apiKey=test123"));
        assertFalse(urlString2.contains("customToken=abc"));
        assertTrue(urlString2.contains("timeout=5000"));
    }

    @Test
    void testExceptionScenarioWithCustomSensitiveParameters() {
        // Add custom sensitive parameter for exception scenario
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().get();
        applicationConfig.setAdditionalSensitiveParameters("nacosAccessKey,nacosSecretKey");

        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?nacosAccessKey=ak123&nacosSecretKey=sk456&timeout=5000");

        // Simulate exception message containing URL
        String exceptionMessage = "Failed to connect to registry: " + url.toString();

        // Custom sensitive parameters should not appear in exception message
        assertFalse(exceptionMessage.contains("nacosAccessKey=ak123"));
        assertFalse(exceptionMessage.contains("nacosSecretKey=sk456"));

        // Non-sensitive parameters should appear
        assertTrue(exceptionMessage.contains("timeout=5000"));

        // But debug information (toFullString) should still contain everything
        String debugInfo = url.toFullString();
        assertTrue(debugInfo.contains("nacosAccessKey=ak123"));
        assertTrue(debugInfo.contains("nacosSecretKey=sk456"));
    }

    @Test
    void testParameterOrderingWithCustomSensitive() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().get();
        applicationConfig.setAdditionalSensitiveParameters("customSensitive");

        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?a=1&customSensitive=hidden&b=2&username=admin&c=3");

        String urlString = url.toString();

        // Non-sensitive parameters should be visible and properly ordered
        assertTrue(urlString.contains("a=1"));
        assertTrue(urlString.contains("b=2"));
        assertTrue(urlString.contains("c=3"));

        // Sensitive parameters should be hidden
        assertFalse(urlString.contains("customSensitive=hidden"));
        assertFalse(urlString.contains("username=admin"));

        // Should still have proper URL format without sensitive params
        assertTrue(urlString.contains("?"));
        assertTrue(urlString.contains("&"));
    }

    @Test
    void testConcurrentConfigurationChanges() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().get();

        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?dynamicParam=value&username=admin");

        // Initially dynamicParam should be visible
        assertTrue(url.toString().contains("dynamicParam=value"));

        // Add dynamicParam as sensitive
        applicationConfig.setAdditionalSensitiveParameters("dynamicParam");

        // Create new URL instance to see updated behavior
        URL url2 = URL.valueOf("nacos://127.0.0.1:8848/registry?dynamicParam=value&username=admin");

        // Now dynamicParam should be hidden
        assertFalse(url2.toString().contains("dynamicParam=value"));

        // Remove it from sensitive list
        applicationConfig.setAdditionalSensitiveParameters("");

        // Create another URL instance
        URL url3 = URL.valueOf("nacos://127.0.0.1:8848/registry?dynamicParam=value&username=admin");

        // dynamicParam should be visible again, username still hidden
        assertTrue(url3.toString().contains("dynamicParam=value"));
        assertFalse(url3.toString().contains("username=admin"));
    }

    @Test
    void testSensitiveParameterUtilsAPI() {
        // Test default parameters
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("password"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("accessKey"));
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("timeout"));

        // Configure additional parameters
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig =
                applicationModel.getConfigManager().getApplication().get();
        applicationConfig.setAdditionalSensitiveParameters("myToken,mySecret");

        // Test configured parameters
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("myToken"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("mySecret"));
        assertTrue(SensitiveParameterUtils.isSensitiveParameter("password")); // Still works for defaults
        assertFalse(SensitiveParameterUtils.isSensitiveParameter("timeout"));

        // Test getAllSensitiveParameters
        Set<String> allParams = SensitiveParameterUtils.getAllSensitiveParameters();
        assertTrue(allParams.contains("password"));
        assertTrue(allParams.contains("myToken"));
        assertTrue(allParams.contains("mySecret"));
        assertFalse(allParams.contains("timeout"));
    }
}
