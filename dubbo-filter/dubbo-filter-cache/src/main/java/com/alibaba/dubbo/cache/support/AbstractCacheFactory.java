/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.cache.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.CacheFactory;
import com.alibaba.dubbo.common.URL;

/**
 * AbstractCacheFactory
 * 
 * @author william.liangf
 */
public abstract class AbstractCacheFactory implements CacheFactory {
    
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

    public Cache getCache(URL url) {
        String key = url.toFullString();
        Cache cache = caches.get(key);
        if (cache == null) {
            caches.put(key, createCache(url));
            cache = caches.get(key);
        }
        return cache;
    }

    protected abstract Cache createCache(URL url);

}
