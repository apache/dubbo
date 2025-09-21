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

import org.apache.dubbo.common.config.SensitiveParameterConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class URLConfigurableSensitiveParametersTest {

    @BeforeEach
    void setUp() {
        // Reset configuration before each test
        SensitiveParameterConfig.resetToDefaults();
        System.clearProperty(SensitiveParameterConfig.SENSITIVE_PARAMS_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        SensitiveParameterConfig.resetToDefaults();
        System.clearProperty(SensitiveParameterConfig.SENSITIVE_PARAMS_PROPERTY);
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
        // Add custom sensitive parameter
        SensitiveParameterConfig.addSensitiveParameters("apiKey", "clientSecret");

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
    void testSystemPropertyConfiguration() {
        // Configure custom sensitive parameters via system property
        System.setProperty(SensitiveParameterConfig.SENSITIVE_PARAMS_PROPERTY, "customToken,bearerToken");

        // Reset to reload configuration
        SensitiveParameterConfig.resetToDefaults();

        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?username=admin&customToken=abc123&bearerToken=xyz789&timeout=5000");

        String urlString = url.toString();

        // All sensitive parameters should be hidden (default + system property)
        assertFalse(urlString.contains("username=admin"));
        assertFalse(urlString.contains("customToken=abc123"));
        assertFalse(urlString.contains("bearerToken=xyz789"));

        // Non-sensitive parameters should be visible
        assertTrue(urlString.contains("timeout=5000"));
    }

    @Test
    void testRemovingCustomSensitiveParameters() {
        // Add custom parameters
        SensitiveParameterConfig.addSensitiveParameters("apiKey", "token");

        URL url1 = URL.valueOf("nacos://127.0.0.1:8848/registry?apiKey=test123&token=abc&timeout=5000");
        String urlString1 = url1.toString();

        // Should be hidden initially
        assertFalse(urlString1.contains("apiKey=test123"));
        assertFalse(urlString1.contains("token=abc"));

        // Remove one custom parameter
        SensitiveParameterConfig.removeSensitiveParameters("apiKey");

        URL url2 = URL.valueOf("nacos://127.0.0.1:8848/registry?apiKey=test123&token=abc&timeout=5000");
        String urlString2 = url2.toString();

        // apiKey should now be visible, token should still be hidden
        assertTrue(urlString2.contains("apiKey=test123"));
        assertFalse(urlString2.contains("token=abc"));
        assertTrue(urlString2.contains("timeout=5000"));
    }

    @Test
    void testExceptionScenarioWithCustomSensitiveParameters() {
        // Add custom sensitive parameter for exception scenario
        SensitiveParameterConfig.addSensitiveParameters("nacosAccessKey", "nacosSecretKey");

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
        SensitiveParameterConfig.addSensitiveParameters("customSensitive");

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
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?dynamicParam=value&username=admin");

        // Initially dynamicParam should be visible
        assertTrue(url.toString().contains("dynamicParam=value"));

        // Add dynamicParam as sensitive
        SensitiveParameterConfig.addSensitiveParameters("dynamicParam");

        // Create new URL instance to see updated behavior
        URL url2 = URL.valueOf("nacos://127.0.0.1:8848/registry?dynamicParam=value&username=admin");

        // Now dynamicParam should be hidden
        assertFalse(url2.toString().contains("dynamicParam=value"));

        // Remove it from sensitive list
        SensitiveParameterConfig.removeSensitiveParameters("dynamicParam");

        // Create another URL instance
        URL url3 = URL.valueOf("nacos://127.0.0.1:8848/registry?dynamicParam=value&username=admin");

        // dynamicParam should be visible again, username still hidden
        assertTrue(url3.toString().contains("dynamicParam=value"));
        assertFalse(url3.toString().contains("username=admin"));
    }

    @Test
    void testShowSensitiveFlagBehavior() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?username=admin&password=secret&timeout=5000");

        // Test default behavior (showSensitive = false)
        String defaultString = url.toString();
        assertFalse(defaultString.contains("username=admin"));
        assertFalse(defaultString.contains("password=secret"));
        assertTrue(defaultString.contains("timeout=5000"));

        // Test with showSensitive = true (toFullString)
        String fullString = url.toFullString();
        assertTrue(fullString.contains("username=admin"));
        assertTrue(fullString.contains("password=secret"));
        assertTrue(fullString.contains("timeout=5000"));

        // Test toString with parameters (includes)
        String paramString = url.toParameterString();
        assertFalse(paramString.contains("username=admin"));
        assertFalse(paramString.contains("password=secret"));
        assertTrue(paramString.contains("timeout=5000"));
    }

    @Test
    void testMixedSensitiveAndNonSensitiveParameters() {
        SensitiveParameterConfig.addSensitiveParameters("customSensitive");

        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?" + "username=admin&"
                + // default sensitive
                "customSensitive=hidden&"
                + // custom sensitive
                "timeout=5000&"
                + // non-sensitive
                "version=1.0&"
                + // non-sensitive
                "accessKey=mykey"); // default sensitive

        String urlString = url.toString();

        // All sensitive parameters should be hidden
        assertFalse(urlString.contains("username=admin"));
        assertFalse(urlString.contains("customSensitive=hidden"));
        assertFalse(urlString.contains("accessKey=mykey"));

        // Non-sensitive parameters should be visible
        assertTrue(urlString.contains("timeout=5000"));
        assertTrue(urlString.contains("version=1.0"));

        // Full string should show everything
        String fullString = url.toFullString();
        assertTrue(fullString.contains("username=admin"));
        assertTrue(fullString.contains("customSensitive=hidden"));
        assertTrue(fullString.contains("accessKey=mykey"));
        assertTrue(fullString.contains("timeout=5000"));
        assertTrue(fullString.contains("version=1.0"));
    }

    @Test
    void testEmptyParameterHandling() {
        // Test URL with empty parameter values
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?username=&password=secret&timeout=&version=1.0");

        String urlString = url.toString();

        // Empty sensitive parameter should still be hidden
        assertFalse(urlString.contains("username="));
        assertFalse(urlString.contains("password=secret"));

        // Empty non-sensitive parameter should be visible
        assertTrue(urlString.contains("timeout="));
        assertTrue(urlString.contains("version=1.0"));
    }

    @Test
    void testParameterOrderPreservation() {
        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?" + "a=1&username=admin&b=2&password=secret&c=3&timeout=5000");

        String urlString = url.toString();

        // Non-sensitive parameters should maintain relative order
        int aIndex = urlString.indexOf("a=1");
        int bIndex = urlString.indexOf("b=2");
        int cIndex = urlString.indexOf("c=3");
        int timeoutIndex = urlString.indexOf("timeout=5000");

        assertTrue(aIndex >= 0);
        assertTrue(bIndex > aIndex);
        assertTrue(cIndex > bIndex);
        assertTrue(timeoutIndex > cIndex);

        // Sensitive parameters should not appear
        assertFalse(urlString.contains("username=admin"));
        assertFalse(urlString.contains("password=secret"));
    }

    @Test
    void testLogicalOperatorChangeWithSensitiveParameters() {
        // Test the logical condition change: (!isSensitiveParameter(key) || showSensitive)
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?username=admin&password=secret&timeout=5000&retries=3");

        // Case 1: !isSensitiveParameter(key) = true, showSensitive = false
        // Result: true || false = true (parameter should be shown)
        String normalString = url.toString();
        assertTrue(normalString.contains("timeout=5000"), "Non-sensitive parameter should be visible");
        assertTrue(normalString.contains("retries=3"), "Non-sensitive parameter should be visible");

        // Case 2: !isSensitiveParameter(key) = false, showSensitive = false
        // Result: false || false = false (parameter should be hidden)
        assertFalse(normalString.contains("username=admin"), "Sensitive parameter should be hidden");
        assertFalse(normalString.contains("password=secret"), "Sensitive parameter should be hidden");

        // Case 3: !isSensitiveParameter(key) = false, showSensitive = true
        // Result: false || true = true (parameter should be shown)
        String fullString = url.toFullString();
        assertTrue(fullString.contains("username=admin"), "Sensitive parameter should be visible in full string");
        assertTrue(fullString.contains("password=secret"), "Sensitive parameter should be visible in full string");

        // Case 4: !isSensitiveParameter(key) = true, showSensitive = true
        // Result: true || true = true (parameter should be shown)
        assertTrue(fullString.contains("timeout=5000"), "Non-sensitive parameter should be visible in full string");
        assertTrue(fullString.contains("retries=3"), "Non-sensitive parameter should be visible in full string");
    }

    @Test
    void testShortCircuitEvaluationBehavior() {
        // Add a custom sensitive parameter
        SensitiveParameterConfig.addSensitiveParameters("customSensitive");

        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?normalParam=value&customSensitive=hidden");

        // Test short-circuit evaluation where first condition is true
        String urlString = url.toString();

        // For normalParam: !isSensitiveParameter("normalParam") = true
        // Short-circuit: true || showSensitive = true (doesn't evaluate showSensitive)
        assertTrue(
                urlString.contains("normalParam=value"),
                "Non-sensitive parameter should be visible due to short-circuit evaluation");

        // For customSensitive: !isSensitiveParameter("customSensitive") = false
        // Must evaluate: false || showSensitive = false (showSensitive is false in toString)
        assertFalse(
                urlString.contains("customSensitive=hidden"),
                "Custom sensitive parameter should be hidden when showSensitive is false");

        // Test with showSensitive = true
        String fullString = url.toFullString();

        // For customSensitive: !isSensitiveParameter("customSensitive") = false
        // Must evaluate: false || showSensitive = true (showSensitive is true in toFullString)
        assertTrue(
                fullString.contains("customSensitive=hidden"),
                "Custom sensitive parameter should be visible when showSensitive is true");
    }

    @Test
    void testBooleanLogicConsistencyAcrossMethods() {
        SensitiveParameterConfig.addSensitiveParameters("apiKey");

        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?apiKey=secret123&timeout=5000");

        // Test toString() - showSensitive = false
        String toStringResult = url.toString();
        assertFalse(toStringResult.contains("apiKey=secret123"), "toString should hide sensitive parameters");
        assertTrue(toStringResult.contains("timeout=5000"), "toString should show non-sensitive parameters");

        // Test toFullString() - showSensitive = true
        String toFullStringResult = url.toFullString();
        assertTrue(toFullStringResult.contains("apiKey=secret123"), "toFullString should show sensitive parameters");
        assertTrue(toFullStringResult.contains("timeout=5000"), "toFullString should show non-sensitive parameters");

        // Test toParameterString() - showSensitive = false
        String toParameterStringResult = url.toParameterString();
        assertFalse(
                toParameterStringResult.contains("apiKey=secret123"),
                "toParameterString should hide sensitive parameters");
        assertTrue(
                toParameterStringResult.contains("timeout=5000"),
                "toParameterString should show non-sensitive parameters");

        // All methods should be logically consistent
        assertEquals(
                toStringResult.contains("apiKey=secret123"),
                toParameterStringResult.contains("apiKey=secret123"),
                "toString and toParameterString should have same sensitive parameter visibility");
    }
}
