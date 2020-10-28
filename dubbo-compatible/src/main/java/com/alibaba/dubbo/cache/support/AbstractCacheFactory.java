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

package com.alibaba.dubbo.cache.support;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.CacheFactory;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;

@Deprecated
public abstract class AbstractCacheFactory implements CacheFactory {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

    @Override
    public Cache getCache(URL url, Invocation invocation) {
        url = url.addParameter(METHOD_KEY, invocation.getMethodName());
        String key = url.toFullString();
        Cache cache = caches.get(key);
        if (cache == null) {
            caches.put(key, createCache(url));
            cache = caches.get(key);
        }
        return cache;
    }

    protected abstract Cache createCache(URL url);

    @Override
    public org.apache.dubbo.cache.Cache getCache(org.apache.dubbo.common.URL url, org.apache.dubbo.rpc.Invocation invocation) {
        return getCache(new URL(url), new Invocation.CompatibleInvocation(invocation));
    }
}
