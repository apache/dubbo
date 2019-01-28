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
package org.apache.dubbo.cache.support.lru;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.LRUCache;

import java.util.Map;

/**
 * This class store the cache value per thread. If a service,method,consumer or provided is configured with key <b>cache</b>
 * with value <b>lru</b>, dubbo initialize the instance of this class using {@link LruCacheFactory} to store method's returns value
 * to server from store without making method call.
 * <pre>
 *     e.g. 1) &lt;dubbo:service cache="lru" cache.size="5000"/&gt;
 *          2) &lt;dubbo:consumer cache="lru" /&gt;
 * </pre>
 * <pre>
 * LruCache uses url's <b>cache.size</b> value for its max store size, if nothing is provided then
 * default value will be 1000
 * </pre>
 *
 * @see Cache
 * @see LruCacheFactory
 * @see org.apache.dubbo.cache.support.AbstractCacheFactory
 * @see org.apache.dubbo.cache.filter.CacheFilter
 */
public class LruCache implements Cache {

    /**
     * This is used to store cache records
     */
    private final Map<Object, Object> store;

    /**
     * Initialize LruCache, it uses constructor argument <b>cache.size</b> value as its storage max size.
     *  If nothing is provided then it will use 1000 as default value.
     * @param url A valid URL instance
     */
    public LruCache(URL url) {
        final int max = url.getParameter("cache.size", 1000);
        this.store = new LRUCache<>(max);
    }

    /**
     * API to store value against a key in the calling thread scope.
     * @param key  Unique identifier for the object being store.
     * @param value Value getting store
     */
    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    /**
     * API to return stored value using a key against the calling thread specific store.
     * @param key Unique identifier for cache lookup
     * @return Return stored object against key
     */
    @Override
    public Object get(Object key) {
        return store.get(key);
    }

}
