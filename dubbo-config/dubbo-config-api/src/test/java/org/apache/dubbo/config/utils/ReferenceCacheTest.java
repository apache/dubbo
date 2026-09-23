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

import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.service.FooService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceCacheTest {

    @BeforeEach
    public void setUp() throws Exception {
        DubboBootstrap.reset();
        SysProps.clear();
        SysProps.setProperty("dubbo.metrics.enabled", "false");
        SysProps.setProperty("dubbo.metrics.protocol", "disabled");
        MockReferenceConfig.setCounter(0);
        XxxMockReferenceConfig.setCounter(0);
        SimpleReferenceCache.CACHE_HOLDER.clear();
    }

    @AfterEach
    public void tearDown() {
        DubboBootstrap.reset();
        SysProps.clear();
        SimpleReferenceCache.CACHE_HOLDER.clear();
    }

    @Test
    void testGetCacheSameReference() {
        ReferenceCache cache = SimpleReferenceCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig();
        assertEquals(0L, config.getCounter());
        Object proxy = cache.get(config);
        assertTrue(config.isGetMethodRun());

        // singleton reference config by default
        MockReferenceConfig configCopy = buildMockReferenceConfig();
        assertEquals(1L, configCopy.getCounter());
        Object proxyOfCopyConfig = cache.get(configCopy);
        assertFalse(configCopy.isGetMethodRun());

        assertEquals(1L, config.getCounter());
        assertEquals(1L, configCopy.getCounter());
        assertEquals(proxy, proxyOfCopyConfig);
    }

    @Test
    void testGetCacheDiffReference() {
        ReferenceCache cache = SimpleReferenceCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig();
        assertEquals(0L, config.getCounter());
        cache.get(config);
        assertEquals(1L, config.getCounter());
        assertTrue(config.isGetMethodRun());
        cache.get(config);
        assertEquals(1L, config.getCounter());

        XxxMockReferenceConfig configCopy = buildXxxMockReferenceConfig();
        assertEquals(0L, configCopy.getCounter());
        cache.get(configCopy);
        assertTrue(configCopy.isGetMethodRun());
        assertEquals(1L, configCopy.getCounter());
    }

    @Test
    void testGetCacheWithKey() {
        ReferenceCache cache = SimpleReferenceCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig();
        FooService value = cache.get(config);
        assertEquals(
                value, cache.get("group1/org.apache.dubbo.config.utils.service.FooService:1.0.0", FooService.class));
    }

    @Test
    void testGetCacheDiffName() {
        SimpleReferenceCache cache = SimpleReferenceCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig();
        assertEquals(0L, config.getCounter());
        cache.get(config);
        assertTrue(config.isGetMethodRun());
        assertEquals(1L, config.getCounter());

        cache = SimpleReferenceCache.getCache("foo");
        config = buildMockReferenceConfig();
        assertEquals(1L, config.getCounter());
        cache.get(config);
        // still init for the same ReferenceConfig if the cache is different
        assertTrue(config.isGetMethodRun());
        assertEquals(2L, config.getCounter());
    }

    @Test
    void testDestroy() {
        SimpleReferenceCache cache = SimpleReferenceCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig();
        cache.get(config);
        XxxMockReferenceConfig configCopy = buildXxxMockReferenceConfig();
        cache.get(configCopy);
        assertEquals(2, cache.getReferenceMap().size());
        cache.destroy(config);
        assertTrue(config.isDestroyMethodRun());
        assertEquals(1, cache.getReferenceMap().size());
        cache.destroy(configCopy);
        assertTrue(configCopy.isDestroyMethodRun());
        assertEquals(0, cache.getReferenceMap().size());
    }

    @Test
    void testDestroyAll() {
        SimpleReferenceCache cache = SimpleReferenceCache.getCache();
        MockReferenceConfig config = buildMockReferenceConfig();
        cache.get(config);
        XxxMockReferenceConfig configCopy = buildXxxMockReferenceConfig();
        cache.get(configCopy);
        assertEquals(2, cache.getReferenceMap().size());
        cache.destroyAll();
        assertTrue(config.isDestroyMethodRun());
        assertTrue(configCopy.isDestroyMethodRun());
        assertEquals(0, cache.getReferenceMap().size());
    }

    private MockReferenceConfig buildMockReferenceConfig() {
        MockReferenceConfig config = new MockReferenceConfig();
        config.setApplication(new ApplicationConfig("cache"));
        config.setRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
        config.setCheck(false);
        config.setInterface("org.apache.dubbo.config.utils.service.FooService");
        config.setGroup("group1");
        config.setVersion("1.0.0");
        return config;
    }

    private XxxMockReferenceConfig buildXxxMockReferenceConfig() {
        XxxMockReferenceConfig config = new XxxMockReferenceConfig();
        config.setApplication(new ApplicationConfig("cache"));
        config.setRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
        config.setInterface("org.apache.dubbo.config.utils.service.XxxService");
        config.setCheck(false);
        config.setGroup("group1");
        config.setVersion("1.0.0");
        return config;
    }

    @Test
    void testKeyGenerationWithRegistry() {
        // Mock two ReferenceConfigs with same Interface/Group/Version
        ReferenceConfigBase<?> rc1 = Mockito.mock(ReferenceConfigBase.class);
        ReferenceConfigBase<?> rc2 = Mockito.mock(ReferenceConfigBase.class);

        Mockito.when(rc1.getInterface()).thenReturn("org.apache.dubbo.config.utils.service.FooService");
        Mockito.when(rc1.getGroup()).thenReturn("group1");
        Mockito.when(rc1.getVersion()).thenReturn("1.0.0");

        Mockito.when(rc2.getInterface()).thenReturn("org.apache.dubbo.config.utils.service.FooService");
        Mockito.when(rc2.getGroup()).thenReturn("group1");
        Mockito.when(rc2.getVersion()).thenReturn("1.0.0");

        Mockito.when(rc1.getRegistryIds()).thenReturn("registry-A");
        Mockito.when(rc2.getRegistryIds()).thenReturn("registry-B");

        String key1 = SimpleReferenceCache.DEFAULT_KEY_GENERATOR.generateKey(rc1);
        String key2 = SimpleReferenceCache.DEFAULT_KEY_GENERATOR.generateKey(rc2);

        assertNotEquals(key1, key2, "Keys should be different for different registries!");

        assertTrue(key1.contains("@registry=registry-A"));
        assertTrue(key2.contains("@registry=registry-B"));
    }
}
