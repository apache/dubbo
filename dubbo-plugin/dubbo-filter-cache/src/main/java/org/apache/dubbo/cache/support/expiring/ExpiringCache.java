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
import org.apache.dubbo.common.URL;

import java.util.Map;

/**
 * ExpiringCache - With the characteristic of expiration time.
 */

/**
 * This class store the cache value with the characteristic of expiration time. If a service,method,consumer or provided is configured with key <b>cache</b>
 * with value <b>expiring</b>, dubbo initialize the instance of this class using {@link ExpiringCacheFactory} to store method's returns value
 * to server from store without making method call.
 * <pre>
 *     e.g. 1) &lt;dubbo:service cache="expiring" cache.seconds="60" cache.interval="10"/&gt;
 *          2) &lt;dubbo:consumer cache="expiring" /&gt;
 * </pre>
 * <li>It used constructor argument url instance <b>cache.seconds</b> value to decide time to live of cached object.Default value of it is 180 second.</li>
 * <li>It used constructor argument url instance <b>cache.interval</b> value for cache value expiration interval.Default value of this is 4 second</li>
 * @see Cache
 * @see ExpiringCacheFactory
 * @see org.apache.dubbo.cache.support.AbstractCacheFactory
 * @see org.apache.dubbo.cache.filter.CacheFilter
 */
public class ExpiringCache implements Cache {
    private final Map<Object, Object> store;

    public ExpiringCache(URL url) {
        // cache time (second)
        final int secondsToLive = url.getParameter("cache.seconds", 180);
        // Cache check interval (second)
        final int intervalSeconds = url.getParameter("cache.interval", 4);
        ExpiringMap<Object, Object> expiringMap = new ExpiringMap<>(secondsToLive, intervalSeconds);
        expiringMap.getExpireThread().startExpiryIfNotStarted();
        this.store = expiringMap;
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
