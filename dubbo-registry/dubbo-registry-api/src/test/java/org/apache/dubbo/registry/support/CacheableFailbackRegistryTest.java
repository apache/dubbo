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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceAddressURL;
import org.apache.dubbo.registry.NotifyListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.URLStrParser.ENCODED_AND_MARK;
import static org.apache.dubbo.common.URLStrParser.ENCODED_QUESTION_MARK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheableFailbackRegistryTest {

    private StubCacheableFailbackRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StubCacheableFailbackRegistry(URL.valueOf("mock://127.0.0.1"));
    }

    @Test
    void shouldRemoveExactTimestamp() throws Exception {
        // Test removing exact "timestamp" parameter
        String rawProvider = "dubbo://127.0.0.1:20880/demo.Service?timestamp=100&revision=v1";
        String result = strip(rawProvider);
        // After removing timestamp, only revision should remain
        assertTrue(result.contains("revision=v1"), "Result should contain revision");
        assertFalse(result.contains("timestamp"), "Result should not contain timestamp");
    }

    @Test
    void shouldRemoveExactPid() throws Exception {
        // Test removing exact "pid" parameter
        String rawProvider = "dubbo://127.0.0.1:20880/demo.Service?pid=9999&revision=v1";
        String result = strip(rawProvider);
        assertTrue(result.contains("revision=v1"), "Result should contain revision");
        assertFalse(result.contains("pid"), "Result should not contain pid");
    }

    @Test
    void shouldRemoveTimestampAndPid() throws Exception {
        // Test removing both timestamp and pid
        String rawProvider = "dubbo://127.0.0.1:20880/demo.Service?timestamp=100&pid=1234&revision=abc";
        String result = strip(rawProvider);
        assertTrue(result.contains("revision=abc"), "Result should contain revision");
        assertFalse(result.contains("timestamp"), "Result should not contain timestamp");
        assertFalse(result.contains("pid"), "Result should not contain pid");
    }

    @Test
    void shouldKeepRemoteTimestamp() throws Exception {
        // remote.timestamp should NOT be removed (only exact "timestamp" is removed)
        String rawProvider = "dubbo://127.0.0.1:20880/demo.Service?remote.timestamp=200&revision=v1";
        String result = strip(rawProvider);
        assertTrue(result.contains("remote.timestamp=200"), "Result should keep remote.timestamp");
        assertTrue(result.contains("revision=v1"), "Result should contain revision");
    }

    @Test
    void shouldRemoveEncodedTimestampAndPid() throws Exception {
        // Test with encoded format
        String base = "dubbo%3A%2F%2F127.0.0.1%3A20880%2Fdemo.Service";
        String rawProvider = base + ENCODED_QUESTION_MARK
                + "timestamp%3D123" + ENCODED_AND_MARK
                + "pid%3D9999" + ENCODED_AND_MARK
                + "revision%3Dv1";
        String result = strip(rawProvider);
        assertTrue(result.contains("revision%3Dv1"), "Result should contain revision");
        assertFalse(result.contains("timestamp%3D"), "Result should not contain timestamp");
        assertFalse(result.contains("pid%3D"), "Result should not contain pid");
    }

    @Test
    void toUrlsWithoutEmptyPreservesTimestampInCachedURL() throws Exception {
        // This test verifies the fix: normalized key for cache, but original rawProvider for URL building.
        String consumerUrl = "mock://127.0.0.1/demo.Service";
        String provider1 = "dubbo://provider1.example.com:20880/demo.Service?timestamp=100&revision=v1";
        String provider2 = "dubbo://provider1.example.com:20880/demo.Service?timestamp=200&revision=v1";
        // Both providers have same address and revision but different timestamp values.
        // After normalization (removing timestamp), they should produce the same cache key.

        URL consumerURL = URL.valueOf(consumerUrl);
        Collection<String> providers = new ArrayList<>();
        providers.add(provider1);
        providers.add(provider2);

        // First call: build cache with both providers
        List<URL> urls1 = registry.toUrlsWithoutEmpty(consumerURL, providers);
        assertNotNull(urls1);
        assertEquals(1, urls1.size());

        // Get the normalized key that should be used
        String normalizedKey1 = strip(provider1); // Should have timestamp removed
        String normalizedKey2 = strip(provider2); // Should be same as normalizedKey1

        assertEquals(normalizedKey1, normalizedKey2, "Normalized keys should be identical after removing timestamp");

        // Verify the cached URL map uses normalized key
        Map<String, ServiceAddressURL> stringUrls = registry.stringUrls.get(consumerURL);
        assertNotNull(stringUrls);
        assertTrue(stringUrls.containsKey(normalizedKey1), "Cache should use normalized key");
        assertEquals(1, stringUrls.size(), "Should have exactly one cache entry for deduplicated provider");
    }

    private String strip(String rawProvider) throws Exception {
        Method method = CacheableFailbackRegistry.class.getDeclaredMethod("stripOffVariableKeys", String.class);
        method.setAccessible(true);
        return (String) method.invoke(registry, rawProvider);
    }

    private static final class StubCacheableFailbackRegistry extends CacheableFailbackRegistry {
        StubCacheableFailbackRegistry(URL url) {
            super(url);
        }

        @Override
        public void doRegister(URL url) {}

        @Override
        public void doUnregister(URL url) {}

        @Override
        public void doSubscribe(URL url, NotifyListener listener) {}

        @Override
        protected boolean isMatch(URL subscribeUrl, URL providerUrl) {
            return true;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }
}
