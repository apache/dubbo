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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ReferenceConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple util class for cache {@link ReferenceConfig}.
 * <p>
 * {@link ReferenceConfig} is a heavy Object, it's necessary to cache these object
 * for the framework which create {@link ReferenceConfig} frequently.
 * <p>
 * You can implement and use your own {@link ReferenceConfig} cache if you need use complicate strategy.
 */
public class ReferenceConfigCache {
    public static final String DEFAULT_NAME = "_DEFAULT_";
    /**
     * Create the key with the <b>Group</b>, <b>Interface</b> and <b>version</b> attribute of {@link ReferenceConfig}.
     * <p>
     * key example: <code>group1/org.apache.dubbo.foo.FooService:1.0.0</code>.
     */
    public static final KeyGenerator DEFAULT_KEY_GENERATOR = referenceConfig -> {
        String iName = referenceConfig.getInterface();
        if (StringUtils.isBlank(iName)) {
            Class<?> clazz = referenceConfig.getInterfaceClass();
            iName = clazz.getName();
        }
        if (StringUtils.isBlank(iName)) {
            throw new IllegalArgumentException("No interface info in ReferenceConfig" + referenceConfig);
        }

        StringBuilder ret = new StringBuilder();
        if (!StringUtils.isBlank(referenceConfig.getGroup())) {
            ret.append(referenceConfig.getGroup()).append("/");
        }
        ret.append(iName);
        if (!StringUtils.isBlank(referenceConfig.getVersion())) {
            ret.append(":").append(referenceConfig.getVersion());
        }
        return ret.toString();
    };
    static final ConcurrentMap<String, ReferenceConfigCache> CACHE_HOLDER = new ConcurrentHashMap<String, ReferenceConfigCache>();
    private final String name;
    private final KeyGenerator generator;
    ConcurrentMap<String, ReferenceConfig<?>> cache = new ConcurrentHashMap<String, ReferenceConfig<?>>();

    private ReferenceConfigCache(String name, KeyGenerator generator) {
        this.name = name;
        this.generator = generator;
    }

    /**
     * Get the cache use default name and {@link #DEFAULT_KEY_GENERATOR} to generate cache key.
     * Create cache if not existed yet.
     */
    public static ReferenceConfigCache getCache() {
        return getCache(DEFAULT_NAME);
    }

    /**
     * Get the cache use specified name and {@link KeyGenerator}.
     * Create cache if not existed yet.
     */
    public static ReferenceConfigCache getCache(String name) {
        return getCache(name, DEFAULT_KEY_GENERATOR);
    }

    /**
     * Get the cache use specified {@link KeyGenerator}.
     * Create cache if not existed yet.
     */
    public static ReferenceConfigCache getCache(String name, KeyGenerator keyGenerator) {
        ReferenceConfigCache cache = CACHE_HOLDER.get(name);
        if (cache != null) {
            return cache;
        }
        CACHE_HOLDER.putIfAbsent(name, new ReferenceConfigCache(name, keyGenerator));
        return CACHE_HOLDER.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ReferenceConfig<T> referenceConfig) {
        String key = generator.generateKey(referenceConfig);

        ReferenceConfig<?> config = cache.get(key);
        if (config != null) {
            return (T) config.get();
        }

        cache.putIfAbsent(key, referenceConfig);
        config = cache.get(key);
        return (T) config.get();
    }

    /**
     * Fetch cache with the specified key. The key is decided by KeyGenerator passed-in. If the default KeyGenerator is
     * used, then the key is in the format of <code>group/interfaceClass:version</code>
     *
     * @param key  cache key
     * @param type object class
     * @param <T>  object type
     * @return object from the cached ReferenceConfig
     * @see KeyGenerator#generateKey(ReferenceConfig)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        ReferenceConfig<?> config = cache.get(key);
        return (config != null) ? (T) config.get() : null;
    }

    void destroyKey(String key) {
        ReferenceConfig<?> config = cache.remove(key);
        if (config == null) {
            return;
        }
        config.destroy();
    }

    /**
     * clear and destroy one {@link ReferenceConfig} in the cache.
     *
     * @param referenceConfig use for create key.
     */
    public <T> void destroy(ReferenceConfig<T> referenceConfig) {
        String key = generator.generateKey(referenceConfig);
        destroyKey(key);
    }

    /**
     * clear and destroy all {@link ReferenceConfig} in the cache.
     */
    public void destroyAll() {
        Set<String> set = new HashSet<String>(cache.keySet());
        for (String key : set) {
            destroyKey(key);
        }
    }

    @Override
    public String toString() {
        return "ReferenceConfigCache(name: " + name
                + ")";
    }

    public interface KeyGenerator {
        String generateKey(ReferenceConfig<?> referenceConfig);
    }
}
