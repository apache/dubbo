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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveParametersIntegrationTest {

    @Test
    void test_nacos_registry_url_with_sensitive_parameters() {
        // Simulate the scenario from the bug report:
        // nacos://mes-nacos-address:8848/org.apache.dubbo.registry.RegistryService?accessKey=...&secretKey=...
        String registryUrlString = "nacos://mes-nacos-address:8848/org.apache.dubbo.registry.RegistryService"
                + "?application=my-app&accessKey=sensitive-access-key&secretKey=sensitive-secret-key&version=1.0.0";

        URL registryUrl = URL.valueOf(registryUrlString);

        // Simulate what happens in AbstractDirectory.list() when throwing RpcException
        String exceptionMessage = "Directory of type ServiceDiscoveryRegistryDirectory already destroyed for service "
                + "com.xxx.dubbo.DemoDubboService:1.0 from registry " + registryUrl;

        // Verify that the exception message does not contain sensitive information
        assertFalse(exceptionMessage.contains("accessKey"), "Exception message should not contain 'accessKey'");
        assertFalse(
                exceptionMessage.contains("sensitive-access-key"),
                "Exception message should not contain access key value");
        assertFalse(exceptionMessage.contains("secretKey"), "Exception message should not contain 'secretKey'");
        assertFalse(
                exceptionMessage.contains("sensitive-secret-key"),
                "Exception message should not contain secret key value");

        // But should contain non-sensitive information
        assertTrue(
                exceptionMessage.contains("nacos://mes-nacos-address:8848"),
                "Exception message should contain registry address");
        assertTrue(
                exceptionMessage.contains("application=my-app"),
                "Exception message should contain non-sensitive parameters");
        assertTrue(exceptionMessage.contains("version=1.0.0"), "Exception message should contain version information");
    }

    @Test
    void test_all_sensitive_parameters_hidden_in_exception_scenario() {
        // Test with all types of sensitive parameters: username, password, accessKey, secretKey
        String registryUrlString = "nacos://user:pass@nacos-server:8848/service"
                + "?application=app&username=dbuser&password=dbpass&accessKey=ak123&secretKey=sk456&timeout=5000";

        URL registryUrl = URL.valueOf(registryUrlString);

        // Simulate exception message that could contain the URL
        String errorMessage = "Failed to connect to registry " + registryUrl + " due to network timeout";

        // Verify all sensitive parameters are hidden
        assertFalse(errorMessage.contains("user:pass@"), "Should not contain URL authority credentials");
        assertFalse(errorMessage.contains("username=dbuser"), "Should not contain username parameter");
        assertFalse(errorMessage.contains("password=dbpass"), "Should not contain password parameter");
        assertFalse(errorMessage.contains("accessKey=ak123"), "Should not contain accessKey parameter");
        assertFalse(errorMessage.contains("secretKey=sk456"), "Should not contain secretKey parameter");

        // Verify non-sensitive parameters are still visible
        assertTrue(errorMessage.contains("nacos-server:8848"), "Should contain server address");
        assertTrue(errorMessage.contains("application=app"), "Should contain application parameter");
        assertTrue(errorMessage.contains("timeout=5000"), "Should contain timeout parameter");
    }

    @Test
    void test_toFullString_still_shows_sensitive_parameters_for_debugging() {
        // Verify that toFullString() still shows sensitive parameters for debugging purposes
        String urlString = "nacos://127.0.0.1:8848/service?accessKey=debug-ak&secretKey=debug-sk&app=test";
        URL url = URL.valueOf(urlString);

        String fullString = url.toFullString();

        // toFullString() should show all parameters including sensitive ones for debugging
        assertTrue(fullString.contains("accessKey=debug-ak"), "toFullString() should show accessKey for debugging");
        assertTrue(fullString.contains("secretKey=debug-sk"), "toFullString() should show secretKey for debugging");

        // But toString() should hide them
        String normalString = url.toString();
        assertFalse(normalString.contains("accessKey"), "toString() should hide accessKey");
        assertFalse(normalString.contains("secretKey"), "toString() should hide secretKey");
    }
}
