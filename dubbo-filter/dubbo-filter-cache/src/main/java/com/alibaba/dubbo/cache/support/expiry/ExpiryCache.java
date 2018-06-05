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
package com.alibaba.dubbo.cache.support.expiry;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.common.URL;

import java.util.Map;

/**
 * ExpiryCache - With the characteristic of expiration time.
 */
public class ExpiryCache implements Cache {
    private final Map<Object, Object> store;

    public ExpiryCache(URL url) {
        // cache time (second)
        final int secondsToLive = url.getParameter("cache.seconds", 180);
        // Cache check interval (second)
        final int intervalSeconds = url.getParameter("cache.interval", 1);
        ExpiryMap<Object, Object> expiryMap = new ExpiryMap<Object, Object>(secondsToLive, intervalSeconds);
        expiryMap.getExpireThread().startExpiryIfNotStarted();
        this.store = expiryMap;
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }

    @Override
    public void destroy() {
        store.clear();
    }
}
