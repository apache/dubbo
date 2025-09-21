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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class URLSensitiveParameterTest {

    @BeforeEach
    void setUp() {
        // Clear any existing system property
        System.clearProperty("dubbo.url.sensitive-parameter-names");
    }

    @AfterEach
    void tearDown() {
        // Clean up after test
        System.clearProperty("dubbo.url.sensitive-parameter-names");
    }

    @Test
    void testDefaultSensitiveParametersInURL() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?password=secret&secretKey=mysecret&timeout=5000");

        String urlString = url.toString();

        // Default sensitive parameters should be hidden
        assertFalse(urlString.contains("password=secret"));
        assertFalse(urlString.contains("secretKey=mysecret"));

        // Non-sensitive parameters should be visible
        assertTrue(urlString.contains("timeout=5000"));

        // toFullString should show everything
        String fullString = url.toFullString();
        assertTrue(fullString.contains("password=secret"));
        assertTrue(fullString.contains("secretKey=mysecret"));
        assertTrue(fullString.contains("timeout=5000"));
    }

    @Test
    void testCustomSensitiveParametersViaSystemProperty() {
        // Configure custom sensitive parameters via system property
        System.setProperty("dubbo.url.sensitive-parameter-names", "customToken,bearerToken");

        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?password=secret&customToken=abc123&bearerToken=xyz789&timeout=5000");

        String urlString = url.toString();

        // All sensitive parameters should be hidden (default + system property)
        assertFalse(urlString.contains("password=secret"));
        assertFalse(urlString.contains("customToken=abc123"));
        assertFalse(urlString.contains("bearerToken=xyz789"));

        // Non-sensitive parameters should be visible
        assertTrue(urlString.contains("timeout=5000"));

        // toFullString should show everything
        String fullString = url.toFullString();
        assertTrue(fullString.contains("password=secret"));
        assertTrue(fullString.contains("customToken=abc123"));
        assertTrue(fullString.contains("bearerToken=xyz789"));
        assertTrue(fullString.contains("timeout=5000"));
    }

    @Test
    void testParameterOrderingWithSensitive() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?a=1&secretKey=hidden&b=2&password=admin&c=3");

        String urlString = url.toString();

        // Non-sensitive parameters should be visible and properly ordered
        assertTrue(urlString.contains("a=1"));
        assertTrue(urlString.contains("b=2"));
        assertTrue(urlString.contains("c=3"));

        // Sensitive parameters should be hidden
        assertFalse(urlString.contains("secretKey=hidden"));
        assertFalse(urlString.contains("password=admin"));

        // Should still have proper URL format without sensitive params
        assertTrue(urlString.contains("?"));
        assertTrue(urlString.contains("&"));
    }

    @Test
    void testToStringWithSpecificParameters() {
        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?password=secret&secretKey=mysecret&timeout=5000&version=1.0");

        // Test toString with specific parameters
        String specificParamsString = url.toString("timeout", "version");
        assertTrue(specificParamsString.contains("timeout=5000"));
        assertTrue(specificParamsString.contains("version=1.0"));
        assertFalse(specificParamsString.contains("password=secret"));
        assertFalse(specificParamsString.contains("secretKey=mysecret"));
    }

    @Test
    void testToParameterString() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?password=secret&secretKey=mysecret&timeout=5000");

        // Test toParameterString - should hide sensitive parameters
        String paramString = url.toParameterString();
        assertTrue(paramString.contains("timeout=5000"));
        assertFalse(paramString.contains("password=secret"));
        assertFalse(paramString.contains("secretKey=mysecret"));
    }

    @Test
    void testToParameterStringWithSpecificParameters() {
        URL url = URL.valueOf(
                "nacos://127.0.0.1:8848/registry?password=secret&secretKey=mysecret&timeout=5000&version=1.0");

        // Test toParameterString with specific parameters
        String paramString = url.toParameterString("timeout", "password");
        assertTrue(paramString.contains("timeout=5000"));
        // password should not be shown even when explicitly requested in toString() mode
        assertFalse(paramString.contains("password=secret"));
        assertFalse(paramString.contains("version=1.0"));
    }

    @Test
    void testUrlWithNoParameters() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry");

        String urlString = url.toString();
        assertFalse(urlString.contains("?"));
        assertFalse(urlString.contains("&"));
        assertEquals("nacos://127.0.0.1:8848/registry", urlString);
    }

    @Test
    void testUrlWithOnlySensitiveParameters() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?password=secret&secretKey=mysecret");

        String urlString = url.toString();
        // Should only have protocol, host, port and path - no query parameters
        assertEquals("nacos://127.0.0.1:8848/registry", urlString);

        // But toFullString should show everything
        String fullString = url.toFullString();
        assertTrue(fullString.contains("password=secret"));
        assertTrue(fullString.contains("secretKey=mysecret"));
    }

    @Test
    void testUrlWithEmptyParameterValues() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?password=&timeout=5000&secretKey=");

        String urlString = url.toString();
        assertTrue(urlString.contains("timeout=5000"));
        // Empty sensitive parameters should still be filtered
        assertFalse(urlString.contains("password="));
        assertFalse(urlString.contains("secretKey="));
    }

    @Test
    void testConfigurationUtilsDirectly() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry");

        // Test ConfigurationUtils methods directly
        assertTrue(org.apache.dubbo.common.config.ConfigurationUtils.isSensitiveParameter(url, "password"));
        assertTrue(org.apache.dubbo.common.config.ConfigurationUtils.isSensitiveParameter(url, "secretKey"));
        assertFalse(org.apache.dubbo.common.config.ConfigurationUtils.isSensitiveParameter(url, "timeout"));
        assertFalse(org.apache.dubbo.common.config.ConfigurationUtils.isSensitiveParameter(url, "version"));
        assertFalse(org.apache.dubbo.common.config.ConfigurationUtils.isSensitiveParameter(url, null));
        assertFalse(org.apache.dubbo.common.config.ConfigurationUtils.isSensitiveParameter(url, ""));
    }

    @Test
    void testSystemPropertyOverride() {
        // Test replacing default sensitive parameters completely
        System.setProperty("dubbo.url.sensitive-parameter-names", "apiKey");

        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?password=secret&apiKey=abc123&timeout=5000");

        String urlString = url.toString();

        // With our current implementation, system property adds to defaults, so both should be hidden
        assertFalse(urlString.contains("password=secret")); // default sensitive parameter
        assertFalse(urlString.contains("apiKey=abc123")); // system property sensitive parameter
        assertTrue(urlString.contains("timeout=5000")); // non-sensitive parameter
    }
}
