/*
 * Copyright 1999-2011 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author ding.lid
 */
public class ReferenceConfigCacheTest {
    @Before
    public void setUp() throws Exception {
        MockReferenceConfig.setCounter(0);
        ReferenceConfigCache.cacheHolder.clear();
    }

    @Test
    public void testGetCache_SameReference() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();

        {
            MockReferenceConfig config = new MockReferenceConfig();
            config.setInterface("FooService");
            config.setGroup("group1");
            config.setVersion("1.0.0");

            String value = cache.get(config);
            assertTrue(config.isGetMethodRun());
            assertEquals("0", value);
        }

        {
            MockReferenceConfig configCopy = new MockReferenceConfig();
            configCopy.setInterface("FooService");
            configCopy.setGroup("group1");
            configCopy.setVersion("1.0.0");

            String value = cache.get(configCopy);
            assertFalse(configCopy.isGetMethodRun());
            assertEquals("0", value);
        }
    }

    @Test
    public void testGetCache_DiffReference() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();

        {
            MockReferenceConfig config = new MockReferenceConfig();
            config.setInterface("FooService");
            config.setGroup("group1");
            config.setVersion("1.0.0");

            String value = cache.get(config);
            assertTrue(config.isGetMethodRun());
            assertEquals("0", value);
        }

        {
            MockReferenceConfig configCopy = new MockReferenceConfig();
            configCopy.setInterface("XxxService");
            configCopy.setGroup("group1");
            configCopy.setVersion("1.0.0");

            String value = cache.get(configCopy);
            assertTrue(configCopy.isGetMethodRun());
            assertEquals("1", value);
        }
    }

    @Test
    public void testGetCache_DiffName() throws Exception {
        {
            ReferenceConfigCache cache = ReferenceConfigCache.getCache();

            MockReferenceConfig config = new MockReferenceConfig();
            config.setInterface("FooService");
            config.setGroup("group1");
            config.setVersion("1.0.0");

            String value = cache.get(config);
            assertTrue(config.isGetMethodRun());
            assertEquals("0", value);
        }
        {
            ReferenceConfigCache cache = ReferenceConfigCache.getCache("foo");

            MockReferenceConfig config = new MockReferenceConfig();
            config.setInterface("FooService");
            config.setGroup("group1");
            config.setVersion("1.0.0");

            String value = cache.get(config);
            assertTrue(config.isGetMethodRun()); // 不同的Cache，相同的ReferenceConfig也会Init
            assertEquals("1", value);
        }
    }

    @Test
    public void testDestroy() throws Exception {
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();

        MockReferenceConfig config = new MockReferenceConfig();
        config.setInterface("FooService");
        config.setGroup("group1");
        config.setVersion("1.0.0");
        cache.get(config);
        MockReferenceConfig configCopy = new MockReferenceConfig();
        configCopy.setInterface("XxxService");
        configCopy.setGroup("group1");
        configCopy.setVersion("1.0.0");
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

        MockReferenceConfig config = new MockReferenceConfig();
        config.setInterface("FooService");
        config.setGroup("group1");
        config.setVersion("1.0.0");
        cache.get(config);
        MockReferenceConfig configCopy = new MockReferenceConfig();
        configCopy.setInterface("XxxService");
        configCopy.setGroup("group1");
        configCopy.setVersion("1.0.0");
        cache.get(configCopy);

        assertEquals(2, cache.cache.size());

        cache.destroyAll();
        assertTrue(config.isDestroyMethodRun());
        assertTrue(configCopy.isDestroyMethodRun());
        assertEquals(0, cache.cache.size());
    }
}
