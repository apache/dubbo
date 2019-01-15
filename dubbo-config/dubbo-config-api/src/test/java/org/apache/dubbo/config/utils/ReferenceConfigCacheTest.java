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
package org.apache.dubbo.config.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReferenceConfigCacheTest {
    @BeforeEach
    public void setUp() throws Exception {
        MockReferenceConfig.setCounter(0);
        ReferenceConfigCache.cacheHolder.clear();
    }

    @Test
    public void testGetCacheSameReference() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig("FooService", "group1", "1.0.0");
        String value = cache.get(config);
        assertTrue(config.isGetMethodRun());
        assertEquals("0", value);

        MockReferenceConfig configCopy = buildMockReferenceConfig("FooService", "group1", "1.0.0");
        value = cache.get(configCopy);
        assertFalse(configCopy.isGetMethodRun());
        assertEquals("0", value);
    }

    @Test
    public void testGetCacheDiffReference() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig("FooService", "group1", "1.0.0");
        String value = cache.get(config);
        assertTrue(config.isGetMethodRun());
        assertEquals("0", value);

        MockReferenceConfig configCopy = buildMockReferenceConfig("XxxService", "group1", "1.0.0");
        value = cache.get(configCopy);
        assertTrue(configCopy.isGetMethodRun());
        assertEquals("1", value);
    }

    @Test
    public void testGetCacheDiffName() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig("FooService", "group1", "1.0.0");
        String value = cache.get(config);
        assertTrue(config.isGetMethodRun());
        assertEquals("0", value);

        cache = ReferenceConfigCache.getCache("foo");
        config = buildMockReferenceConfig("FooService", "group1", "1.0.0");
        value = cache.get(config);
        // still init for the same ReferenceConfig if the cache is different
        assertTrue(config.isGetMethodRun());
        assertEquals("1", value);
    }

    @Test
    public void testDestroy() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig("FooService", "group1", "1.0.0");
        cache.get(config);
        MockReferenceConfig configCopy = buildMockReferenceConfig("XxxService", "group1", "1.0.0");
        cache.get(configCopy);
        assertEquals(2, cache.cache.size());
        cache.destroy(config);
        assertTrue(config.isDestroyMethodRun());
        assertEquals(1, cache.cache.size());
        cache.destroy(configCopy);
        assertTrue(configCopy.isDestroyMethodRun());
        assertEquals(0, cache.cache.size());
    }

    @Test
    public void testDestroyAll() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig("FooService", "group1", "1.0.0");
        cache.get(config);
        MockReferenceConfig configCopy = buildMockReferenceConfig("XxxService", "group1", "1.0.0");
        cache.get(configCopy);
        assertEquals(2, cache.cache.size());
        cache.destroyAll();
        assertTrue(config.isDestroyMethodRun());
        assertTrue(configCopy.isDestroyMethodRun());
        assertEquals(0, cache.cache.size());
    }

    private MockReferenceConfig buildMockReferenceConfig(String service, String group, String version) {
        MockReferenceConfig config = new MockReferenceConfig();
        config.setInterface(service);
        config.setGroup(group);
        config.setVersion(version);
        return config;
    }
}
