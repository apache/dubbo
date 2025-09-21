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
package org.apache.dubbo.config;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

/**
 * Test for MSE Nacos features in RegistryConfig without external dependencies
 */
class RegistryConfigMseNacosTest {

    @BeforeEach
    public void beforeEach() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        DubboBootstrap.reset();
    }

    @Test
    void testMseNacosNamespace() {
        RegistryConfig registry = new RegistryConfig();
        registry.setNamespace("test-namespace");
        assertThat(registry.getNamespace(), equalTo("test-namespace"));
    }

    @Test
    void testMseNacosAccessKey() {
        RegistryConfig registry = new RegistryConfig();
        registry.setAccessKey("test-access-key");
        assertThat(registry.getAccessKey(), equalTo("test-access-key"));
    }

    @Test
    void testMseNacosSecretKey() {
        RegistryConfig registry = new RegistryConfig();
        registry.setSecretKey("test-secret-key");
        assertThat(registry.getSecretKey(), equalTo("test-secret-key"));
    }

    @Test
    void testMseNacosAddressWithCredentials() {
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(
                "nacos://127.0.0.1:8848/registry?namespace=test-ns&accessKey=ak123456789&secretKey=sk987654321&timeout=5000");

        assertThat(
                registry.getAddress(),
                equalTo(
                        "nacos://127.0.0.1:8848/registry?namespace=test-ns&accessKey=ak123456789&secretKey=sk987654321&timeout=5000"));
        assertThat(registry.getProtocol(), equalTo("nacos"));
        assertThat(registry.getNamespace(), equalTo("test-ns"));
        assertThat(registry.getAccessKey(), equalTo("ak123456789"));
        assertThat(registry.getSecretKey(), equalTo("sk987654321"));
        assertThat(registry.getTimeout(), equalTo(5000));

        // Verify that sensitive parameters are removed from the parameters map
        Map<String, String> parameters = registry.getParameters();
        assertThat(parameters, not(hasKey("accessKey")));
        assertThat(parameters, not(hasKey("secretKey")));
        assertThat(parameters, hasEntry("namespace", "test-ns"));
    }

    @Test
    void testSafeCredentialInfo() {
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("nacos://127.0.0.1:8848");
        registry.setNamespace("test-namespace");
        registry.setAccessKey("ak123456789012345");
        registry.setSecretKey("sk987654321098765");
        registry.setUsername("testuser");

        String safeInfo = registry.getSafeCredentialInfo();

        // Should contain non-sensitive information
        assertThat(safeInfo, org.hamcrest.CoreMatchers.containsString("address=nacos://127.0.0.1:8848"));
        assertThat(safeInfo, org.hamcrest.CoreMatchers.containsString("namespace=test-namespace"));
        assertThat(safeInfo, org.hamcrest.CoreMatchers.containsString("username=testuser"));

        // Should mask sensitive information
        assertThat(safeInfo, org.hamcrest.CoreMatchers.containsString("accessKey=ak1***345"));
        assertThat(safeInfo, org.hamcrest.CoreMatchers.containsString("secretKey=sk9***765"));

        // Should not contain full sensitive values
        assertThat(safeInfo, not(org.hamcrest.CoreMatchers.containsString("ak123456789012345")));
        assertThat(safeInfo, not(org.hamcrest.CoreMatchers.containsString("sk987654321098765")));
    }

    @Test
    void testSafeCredentialInfoWithShortKeys() {
        RegistryConfig registry = new RegistryConfig();
        registry.setAccessKey("abc");
        registry.setSecretKey("def");

        String safeInfo = registry.getSafeCredentialInfo();

        // Short keys should be completely masked
        assertThat(safeInfo, org.hamcrest.CoreMatchers.containsString("accessKey=***"));
        assertThat(safeInfo, org.hamcrest.CoreMatchers.containsString("secretKey=***"));
    }

    @Test
    void testMseNacosParametersExcluded() {
        RegistryConfig registry = new RegistryConfig();
        registry.setAccessKey("test-access-key");
        registry.setSecretKey("test-secret-key");

        Map<String, String> parameters = new HashMap<>();
        RegistryConfig.appendParameters(parameters, registry);

        // Verify that accessKey and secretKey are excluded from URL parameters
        // due to @Parameter(excluded = true, attribute = false) annotation
        assertThat(parameters, not(hasKey("accessKey")));
        assertThat(parameters, not(hasKey("secretKey")));
    }
}
