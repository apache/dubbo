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

import org.apache.dubbo.common.BaseServiceMetadata;
import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.rpc.service.Destroyable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple util class for cache {@link ReferenceConfigBase}.
 * <p>
 * {@link ReferenceConfigBase} is a heavy Object, it's necessary to cache these object
 * for the framework which create {@link ReferenceConfigBase} frequently.
 * <p>
 * You can implement and use your own {@link ReferenceConfigBase} cache if you need use complicate strategy.
 */
public class SimpleReferenceCache implements ReferenceCache {
    public static final String DEFAULT_NAME = "_DEFAULT_";
    /**
     * Create the key with the <b>Group</b>, <b>Interface</b> and <b>version</b> attribute of {@link ReferenceConfigBase}.
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

        return BaseServiceMetadata.buildServiceKey(iName, referenceConfig.getGroup(), referenceConfig.getVersion());
    };

    private static final AtomicInteger nameIndex = new AtomicInteger();

    static final ConcurrentMap<String, SimpleReferenceCache> CACHE_HOLDER = new ConcurrentHashMap<>();
    private final String name;
    private final KeyGenerator generator;

    private final Map<String, List<ReferenceConfigBase<?>>> referenceKeyMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<ReferenceConfigBase<?>>> referenceTypeMap = new ConcurrentHashMap<>();
    private final Map<ReferenceConfigBase<?>, Object> references = new ConcurrentHashMap<>();

    protected SimpleReferenceCache(String name, KeyGenerator generator) {
        this.name = name;
        this.generator = generator;
    }

    /**
     * Get the cache use default name and {@link #DEFAULT_KEY_GENERATOR} to generate cache key.
     * Create cache if not existed yet.
     */
    public static SimpleReferenceCache getCache() {
        return getCache(DEFAULT_NAME);
    }

    public static SimpleReferenceCache newCache() {
        return getCache(DEFAULT_NAME + "#" + nameIndex.incrementAndGet());
    }

    /**
     * Get the cache use specified name and {@link KeyGenerator}.
     * Create cache if not existed yet.
     */
    public static SimpleReferenceCache getCache(String name) {
        return getCache(name, DEFAULT_KEY_GENERATOR);
    }

    /**
     * Get the cache use specified {@link KeyGenerator}.
     * Create cache if not existed yet.
     */
    public static SimpleReferenceCache getCache(String name, KeyGenerator keyGenerator) {
        return CACHE_HOLDER.computeIfAbsent(name, k -> new SimpleReferenceCache(k, keyGenerator));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ReferenceConfigBase<T> rc) {
        String key = generator.generateKey(rc);
        Class<?> type = rc.getInterfaceClass();
        Object proxy = rc.get();

        references.computeIfAbsent(rc, _rc -> {
            List<ReferenceConfigBase<?>> referencesOfType = referenceTypeMap.computeIfAbsent(type, _t -> Collections.synchronizedList(new ArrayList<>()));
            referencesOfType.add(rc);
            List<ReferenceConfigBase<?>> referenceConfigList = referenceKeyMap.computeIfAbsent(key, _k -> Collections.synchronizedList(new ArrayList<>()));
            referenceConfigList.add(rc);
            return proxy;
        });

        return (T) proxy;
    }

    /**
     * Fetch cache with the specified key. The key is decided by KeyGenerator passed-in. If the default KeyGenerator is
     * used, then the key is in the format of <code>group/interfaceClass:version</code>
     *
     * @param key  cache key
     * @param type object class
     * @param <T>  object type
     * @return object from the cached ReferenceConfigBase
     * @see KeyGenerator#generateKey(ReferenceConfigBase)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        List<ReferenceConfigBase<?>> referenceConfigs = referenceKeyMap.get(key);
        if (CollectionUtils.isNotEmpty(referenceConfigs)) {
            return (T) referenceConfigs.get(0).get();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        List<ReferenceConfigBase<?>> referenceConfigs = referenceKeyMap.get(key);
        if (referenceConfigs != null && referenceConfigs.size() > 0) {
            return (T) referenceConfigs.get(0).get();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getAll(Class<T> type) {
        List<ReferenceConfigBase<?>> referenceConfigBases = referenceTypeMap.get(type);
        if (CollectionUtils.isEmpty(referenceConfigBases)) {
            return Collections.EMPTY_LIST;
        }
        List proxiesOfType = new ArrayList(referenceConfigBases.size());
        for (ReferenceConfigBase<?> rc : referenceConfigBases) {
            proxiesOfType.add(rc.get());
        }
        return Collections.unmodifiableList(proxiesOfType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        List<ReferenceConfigBase<?>> referenceConfigBases = referenceTypeMap.get(type);
        if (referenceConfigBases != null && referenceConfigBases.size() > 0) {
            return (T) referenceConfigBases.get(0).get();
        }
        return null;
    }

    @Override
    public void destroy(String key, Class<?> type) {
        List<ReferenceConfigBase<?>> referencesOfKey = referenceKeyMap.remove(key);
        if (CollectionUtils.isEmpty(referencesOfKey)) {
            return;
        }
        List<ReferenceConfigBase<?>> referencesOfType = referenceTypeMap.get(type);
        if (CollectionUtils.isEmpty(referencesOfType)) {
            return;
        }
        for (ReferenceConfigBase<?> rc : referencesOfKey) {
            referencesOfType.remove(rc);
            destroyReference(rc);
        }

    }

    @Override
    public void destroy(Class<?> type) {
        List<ReferenceConfigBase<?>> referencesOfType = referenceTypeMap.remove(type);
        for (ReferenceConfigBase<?> rc : referencesOfType) {
            String key = generator.generateKey(rc);
            referenceKeyMap.remove(key);
            destroyReference(rc);
        }
    }

    /**
     * clear and destroy one {@link ReferenceConfigBase} in the cache.
     *
     * @param referenceConfig use for create key.
     */
    @Override
    public <T> void destroy(ReferenceConfigBase<T> referenceConfig) {
        String key = generator.generateKey(referenceConfig);
        Class<?> type = referenceConfig.getInterfaceClass();
        destroy(key, type);
    }

    /**
     * clear and destroy all {@link ReferenceConfigBase} in the cache.
     */
    @Override
    public void destroyAll() {
        if (CollectionUtils.isEmptyMap(referenceKeyMap)) {
            return;
        }

        referenceKeyMap.forEach((_k, referencesOfKey) -> {
            for (ReferenceConfigBase<?> rc : referencesOfKey) {
                destroyReference(rc);
            }
        });

        referenceKeyMap.clear();
        referenceTypeMap.clear();
    }

    private void destroyReference(ReferenceConfigBase<?> rc) {
        Destroyable proxy = (Destroyable) rc.get();
        proxy.$destroy();
        rc.destroy();
    }

    public Map<String, List<ReferenceConfigBase<?>>> getReferenceMap() {
        return referenceKeyMap;
    }

    public Map<Class<?>, List<ReferenceConfigBase<?>>> getReferenceTypeMap() {
        return referenceTypeMap;
    }

    @Override
    public String toString() {
        return "ReferenceCache(name: " + name + ")";
    }

    public interface KeyGenerator {
        String generateKey(ReferenceConfigBase<?> referenceConfig);
    }
}
