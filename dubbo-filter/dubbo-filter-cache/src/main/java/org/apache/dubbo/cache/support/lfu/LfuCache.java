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
package org.apache.dubbo.cache.support.lfu;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.LFUCache;

/**
 * This class store the cache value per thread. If a service,method,consumer or provided is configured with key <b>cache</b>
 * with value <b>lfu</b>, dubbo initialize the instance of this class using {@link LfuCacheFactory} to store method's returns value
 * to server from store without making method call.
 * <pre>
 *     e.g. 1) &lt;dubbo:service cache="lfu" cache.size="5000" cache.evictionFactor="0.3"/&gt;
 *          2) &lt;dubbo:consumer cache="lfu" /&gt;
 * </pre>
 * <pre>
 * LfuCache uses url's <b>cache.size</b> value for its max store size, url's <b>cache.evictionFactor</b> value for its eviction factor,
 * default store size value will be 1000, default eviction factor will be 0.3
 * </pre>
 *
 * @see Cache
 * @see LfuCacheFactory
 * @see org.apache.dubbo.cache.support.AbstractCacheFactory
 * @see org.apache.dubbo.cache.filter.CacheFilter
 */
public class LfuCache implements Cache {

    /**
     * This is used to store cache records
     */
    private final LFUCache store;

    /**
     *  Initialize LfuCache, it uses constructor argument <b>cache.size</b> value as its storage max size.
     *  If nothing is provided then it will use 1000 as default size value. <b>cache.evictionFactor</b> value as its eviction factor.
     *  If nothing is provided then it will use 0.3 as default value.
     * @param url A valid URL instance
     */
    public LfuCache (URL url) {
        final int max = url.getParameter("cache.size", 1000);
        final float factor = url.getParameter("cache.evictionFactor", 0.75f);
        this.store = new LFUCache(max, factor);
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
