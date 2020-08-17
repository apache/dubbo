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
package org.apache.dubbo.cache.support;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.CacheFactory;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;

/**
 * AbstractCacheFactory is a default implementation of {@link CacheFactory}. It abstract out the key formation from URL along with
 * invocation method. It initially check if the value for key already present in own local in-memory store then it won't check underlying storage cache {@link Cache}.
 * Internally it used {@link ConcurrentHashMap} to store do level-1 caching.
 *
 * @see CacheFactory
 * @see org.apache.dubbo.cache.support.jcache.JCacheFactory
 * @see org.apache.dubbo.cache.support.lru.LruCacheFactory
 * @see org.apache.dubbo.cache.support.threadlocal.ThreadLocalCacheFactory
 * @see org.apache.dubbo.cache.support.expiring.ExpiringCacheFactory
 */
public abstract class AbstractCacheFactory implements CacheFactory {

    /**
     * This is used to store factory level-1 cached data.
     */
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

    /**
     *  Takes URL and invocation instance and return cache instance for a given url.
     * @param url url of the method
     * @param invocation invocation context.
     * @return Instance of cache store used as storage for caching return values.
     */
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

    /**
     * Takes url as an method argument and return new instance of cache store implemented by AbstractCacheFactory subclass.
     * @param url url of the method
     * @return Create and return new instance of cache store used as storage for caching return values.
     */
    protected abstract Cache createCache(URL url);

}
