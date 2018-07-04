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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

/**
 * JCache
 */
public class JCache implements org.apache.dubbo.cache.Cache {

    private final Cache<Object, Object> store;

    public JCache(URL url) {
        String method = url.getParameter(Constants.METHOD_KEY, "");
        String key = url.getAddress() + "." + url.getServiceKey() + "." + method;
        // jcache parameter is the full-qualified class name of SPI implementation
        String type = url.getParameter("jcache");

        CachingProvider provider = type == null || type.length() == 0 ? Caching.getCachingProvider() : Caching.getCachingProvider(type);
        CacheManager cacheManager = provider.getCacheManager();
        Cache<Object, Object> cache = cacheManager.getCache(key);
        if (cache == null) {
            try {
                //configure the cache
                MutableConfiguration config =
                        new MutableConfiguration<Object, Object>()
                                .setTypes(Object.class, Object.class)
                                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MILLISECONDS, url.getMethodParameter(method, "cache.write.expire", 60 * 1000))))
                                .setStoreByValue(false)
                                .setManagementEnabled(true)
                                .setStatisticsEnabled(true);
                cache = cacheManager.createCache(key, config);
            } catch (CacheException e) {
                // concurrent cache initialization
                cache = cacheManager.getCache(key);
            }
        }

        this.store = cache;
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
