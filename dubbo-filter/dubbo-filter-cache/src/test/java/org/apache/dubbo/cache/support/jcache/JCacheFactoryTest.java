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
package org.apache.dubbo.cache.support.jcache;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.support.AbstractCacheFactory;
import org.apache.dubbo.cache.support.AbstractCacheFactoryTest;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JCacheFactoryTest extends AbstractCacheFactoryTest {

    @Test
    public void testJCacheFactory() throws Exception {
        Cache cache = super.constructCache();
        assertThat(cache instanceof JCache, is(true));
    }

    @Test
    public void testJCacheGetExpired() throws Exception {
        URL url = URL.valueOf("test://test:12/test?cache=jacache&cache.write.expire=1");
        AbstractCacheFactory cacheFactory = getCacheFactory();
        Invocation invocation = new RpcInvocation();
        Cache cache = cacheFactory.getCache(url, invocation);
        cache.put("testKey", "testValue");
        Thread.sleep(10);
        assertNull(cache.get("testKey"));
    }

    @Override
    protected AbstractCacheFactory getCacheFactory() {
        return new JCacheFactory();
    }
}
