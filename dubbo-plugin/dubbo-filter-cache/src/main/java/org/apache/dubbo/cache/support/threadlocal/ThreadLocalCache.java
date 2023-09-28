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
package org.apache.dubbo.cache.support.threadlocal;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.common.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * This class store the cache value per thread. If a service,method,consumer or provided is configured with key <b>cache</b>
 * with value <b>threadlocal</b>, dubbo initialize the instance of this class using {@link ThreadLocalCacheFactory} to store method's returns value
 * to server from store without making method call.
 *  <pre>
 *      e.g. &lt;dubbo:service cache="threadlocal" /&gt;
 *  </pre>
 * <pre>
 * As this ThreadLocalCache stores key-value in memory without any expiry or delete support per thread wise, if number threads and number of key-value are high then jvm should be
 * configured with appropriate memory.
 * </pre>
 *
 * @see org.apache.dubbo.cache.support.AbstractCacheFactory
 * @see org.apache.dubbo.cache.filter.CacheFilter
 * @see Cache
 */
public class ThreadLocalCache implements Cache {

    /**
     * Thread local variable to store cached data.
     */
    private final ThreadLocal<Map<Object, Object>> store;

    /**
     * Taken URL as an argument to create an instance of ThreadLocalCache. In this version of implementation constructor
     * argument is not getting used in the scope of this class.
     * @param url
     */
    public ThreadLocalCache(URL url) {
        this.store = ThreadLocal.withInitial(HashMap::new);
    }

    /**
     * API to store value against a key in the calling thread scope.
     * @param key  Unique identifier for the object being store.
     * @param value Value getting store
     */
    @Override
    public void put(Object key, Object value) {
        store.get().put(key, value);
    }

    /**
     * API to return stored value using a key against the calling thread specific store.
     * @param key Unique identifier for cache lookup
     * @return Return stored object against key
     */
    @Override
    public Object get(Object key) {
        return store.get().get(key);
    }

}
