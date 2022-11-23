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
package org.apache.dubbo.cache.support.expiring;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.support.AbstractCacheFactory;
import org.apache.dubbo.cache.support.AbstractCacheFactoryTest;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExpiringCacheFactoryTest extends AbstractCacheFactoryTest {

    private static final String EXPIRING_CACHE_URL =
            "test://test:12/test?cache=expiring&cache.seconds=1&cache.interval=1";

    @Test
    void testExpiringCacheFactory() throws Exception {
        Cache cache = super.constructCache();
        assertThat(cache instanceof ExpiringCache, is(true));
    }

    @Test
    void testExpiringCacheGetExpired() throws Exception {
        URL url = URL.valueOf("test://test:12/test?cache=expiring&cache.seconds=1&cache.interval=1");
        AbstractCacheFactory cacheFactory = getCacheFactory();
        Invocation invocation = new RpcInvocation();
        Cache cache = cacheFactory.getCache(url, invocation);
        cache.put("testKey", "testValue");
        Thread.sleep(2100);
        assertNull(cache.get("testKey"));
    }

    @Test
    void testExpiringCacheUnExpired() throws Exception {
        URL url = URL.valueOf("test://test:12/test?cache=expiring&cache.seconds=0&cache.interval=1");
        AbstractCacheFactory cacheFactory = getCacheFactory();
        Invocation invocation = new RpcInvocation();
        Cache cache = cacheFactory.getCache(url, invocation);
        cache.put("testKey", "testValue");
        Thread.sleep(1100);
        assertNotNull(cache.get("testKey"));
    }

    @Test
    void testExpiringCache() throws Exception {
        Cache cache = constructCache();
        assertThat(cache instanceof ExpiringCache, is(true));

        // 500ms
        TimeUnit.MILLISECONDS.sleep(500);
        cache.put("testKey", "testValue");
        // 800ms
        TimeUnit.MILLISECONDS.sleep(300);
        assertNotNull(cache.get("testKey"));
        // 1300ms
        TimeUnit.MILLISECONDS.sleep(500);
        assertNotNull(cache.get("testKey"));
    }

    @Test
    void testExpiringCacheExpired() throws Exception {
        Cache cache = constructCache();
        assertThat(cache instanceof ExpiringCache, is(true));

        // 500ms
        TimeUnit.MILLISECONDS.sleep(500);
        cache.put("testKey", "testValue");
        // 1000ms ExpireThread clear all expire cache
        TimeUnit.MILLISECONDS.sleep(500);
        // 1700ms  get should be null
        TimeUnit.MILLISECONDS.sleep(700);
        assertNull(cache.get("testKey"));
    }

    @Override
    protected Cache constructCache() {
        URL url = URL.valueOf(EXPIRING_CACHE_URL);
        Invocation invocation = new RpcInvocation();
        return getCacheFactory().getCache(url, invocation);
    }

    @Override
    protected AbstractCacheFactory getCacheFactory() {
        return new ExpiringCacheFactory();
    }
}