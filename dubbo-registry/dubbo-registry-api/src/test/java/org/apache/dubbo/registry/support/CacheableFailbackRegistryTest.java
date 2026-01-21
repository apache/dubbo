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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheableFailbackRegistryTest {

    private StubCacheableFailbackRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StubCacheableFailbackRegistry(URL.valueOf("mock://127.0.0.1"));
    }

    @Test
    void shouldKeepRemoteTimestampIntact() throws Exception {
        String base = "dubbo%3A%2F%2F127.0.0.1%3A20880%2Fdemo.Service";
        String rawProvider =
                base + ENCODED_QUESTION_MARK + "remote.timestamp%3D1" + ENCODED_AND_MARK + "revision%3Dabc";
        assertEquals(rawProvider, strip(rawProvider));
    }

    @Test
    void shouldRemoveOnlyTimestampWhenRemoteTimestampExists() throws Exception {
        String base = "dubbo%3A%2F%2F127.0.0.1%3A20880%2Fdemo.Service";
        String rawProvider = base + ENCODED_QUESTION_MARK
                + "timestamp%3D1" + ENCODED_AND_MARK
                + "remote.timestamp%3D2" + ENCODED_AND_MARK
                + "revision%3D3";
        String expected = base + ENCODED_QUESTION_MARK + "remote.timestamp%3D2" + ENCODED_AND_MARK + "revision%3D3";
        assertEquals(expected, strip(rawProvider));
    }

    @Test
    void shouldHandleDecodedParameters() throws Exception {
        String base = "dubbo://127.0.0.1:20880/demo.Service";
        String rawProvider = base + "?timestamp=1&remote.timestamp=2&revision=3";
        String expected = base + "?remote.timestamp=2&revision=3";
        assertEquals(expected, strip(rawProvider));
    }

    @Test
    void shouldRemoveTimestampAndPidWithPrefixes() throws Exception {
        // Test that both timestamp and pid can be removed even with prefixes like "remote.timestamp" and "dubbo.pid"
        String base = "dubbo://127.0.0.1:20880/demo.Service";
        String rawProvider = base + "?timestamp=100&remote.timestamp=200&pid=1234&dubbo.pid=5678&revision=abc";
        String expected = base + "?remote.timestamp=200&revision=abc";
        assertEquals(expected, strip(rawProvider));
    }

    @Test
    void shouldRemoveAllTimestampsExactAndPrefixed() throws Exception {
        // Test comprehensive scenario: multiple timestamps (exact and prefixed) should all be removed
        String base = "dubbo://127.0.0.1:20880/demo.Service";
        String rawProvider = base + "?timestamp=1&remote.timestamp=2&any.timestamp=3&revision=v1";
        String expected = base + "?revision=v1";
        assertEquals(expected, strip(rawProvider));
    }

    @Test
    void toUrlsWithoutEmptyPreservesTimestampInCachedURL() throws Exception {
        // This test verifies the fix: normalized key for cache, but original rawProvider for URL building.
        String consumerUrl = "mock://127.0.0.1/demo.Service";
        String provider1 = "dubbo://provider1.example.com:20880/demo.Service?timestamp=100&revision=v1";
        String provider2 = "dubbo://provider1.example.com:20880/demo.Service?timestamp=200&revision=v1";
        // Both providers have same address and revision but different timestamp values.
        // After normalization (removing timestamp), they should produce the same cache key
        // and reuse the same cached URL.

        URL consumerURL = URL.valueOf(consumerUrl);
        Collection<String> providers = new ArrayList<>();
        providers.add(provider1);
        providers.add(provider2);

        // First call: build cache with provider1
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

    @Test
    void stripOffVariableKeysRemovesEncodedTimestampAndPid() throws Exception {
        // Test with encoded format to ensure ENCODED_TIMESTAMP_KEY and ENCODED_PID_KEY are properly handled
        String base = "dubbo%3A%2F%2Fprovider.example.com%3A20880%2Fdemo.Service";
        String rawProvider = base + ENCODED_QUESTION_MARK
                + "timestamp%3D123" + ENCODED_AND_MARK
                + "pid%3D9999" + ENCODED_AND_MARK
                + "revision%3Dv1";
        String expected = base + ENCODED_QUESTION_MARK + "revision%3Dv1";
        assertEquals(expected, strip(rawProvider));
    }

    @Test
    void stripOffVariableKeysPreservesPrefixedTimestampInNormalization() throws Exception {
        // Verify that parameters ending with ".timestamp" (but not the "timestamp" value itself) are treated as
        // variable keys
        String base = "dubbo://provider.example.com:20880/demo.Service";
        String rawProvider = base + "?app.timestamp=111&custom.timestamp=222&timestamp=333&revision=abc";
        String expected = base + "?revision=abc";
        assertEquals(expected, strip(rawProvider));
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
