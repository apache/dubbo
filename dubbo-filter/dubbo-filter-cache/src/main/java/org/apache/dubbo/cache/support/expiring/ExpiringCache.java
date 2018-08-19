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
public class ExpiringCache implements Cache {
    private final Map<Object, Object> store;

    public ExpiringCache(URL url) {
        // cache time (second)
        final int secondsToLive = url.getParameter("cache.seconds", 180);
        // Cache check interval (second)
        final int intervalSeconds = url.getParameter("cache.interval", 4);
        ExpiringMap<Object, Object> expiringMap = new ExpiringMap<Object, Object>(secondsToLive, intervalSeconds);
        expiringMap.getExpireThread().startExpiryIfNotStarted();
        this.store = expiringMap;
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }

}
