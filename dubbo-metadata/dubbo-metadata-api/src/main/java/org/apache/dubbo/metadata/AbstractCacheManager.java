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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.cache.FileCacheStore;
import org.apache.dubbo.common.cache.FileCacheStoreFactory;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_FAILED_LOAD_MAPPING_CACHE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;

public abstract class AbstractCacheManager<V> implements Disposable {
    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    protected FileCacheStore cacheStore;
    protected LRUCache<String, V> cache;
    private List<Disposable> disposableResources = new ArrayList<>();

    protected void init(
            boolean enableFileCache,
            String filePath,
            String fileName,
            int entrySize,
            long fileSize,
            int interval,
            ScheduledExecutorService executorService) {
        this.cache = new LRUCache<>(entrySize);
        registerDisposable(() -> this.cache.clear());

        try {
            cacheStore = FileCacheStoreFactory.getInstance(filePath, fileName, enableFileCache);
            registerDisposable(() -> cacheStore.destroy());

            Map<String, String> properties = cacheStore.loadCache(entrySize);
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully loaded " + getName() + " cache from file " + fileName + ", entries "
                        + properties.size());
            }
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                this.cache.put(key, toValueType(value));
            }
            // executorService can be empty if FileCacheStore fails
            if (executorService == null) {
                executorService = Executors.newSingleThreadScheduledExecutor(
                        new NamedThreadFactory("Dubbo-cache-refreshing-scheduler", true));
                registerDisposable(newExecutorDisposer(executorService));
            }

            final ScheduledFuture<?> newFuture = executorService.scheduleWithFixedDelay(
                    new CacheRefreshTask<>(this.cacheStore, this.cache, this, fileSize),
                    10,
                    interval,
                    TimeUnit.MINUTES);

            registerDisposable(() -> newFuture.cancel(true));

        } catch (Exception e) {
            logger.error(COMMON_FAILED_LOAD_MAPPING_CACHE, "", "", "Load mapping from local cache file error ", e);
        }
    }

    protected void registerDisposable(Disposable resource) {
        this.disposableResources.add(resource);
    }

    private Disposable newExecutorDisposer(final ExecutorService executor) {
        Assert.notNull(executor, "ExecutorService can not be null");
        return () -> {
            // Try to destroy self-created executorService instance.
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(
                        ConfigurationUtils.reCalShutdownTime(DEFAULT_SERVER_SHUTDOWN_TIMEOUT), TimeUnit.MILLISECONDS)) {
                    logger.warn(
                            COMMON_UNEXPECTED_EXCEPTION, "", "", "Wait global executor service terminated timeout.");
                }
            } catch (InterruptedException e) {
                logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", "destroy resources failed: " + e.getMessage(), e);
            }
        };
    }

    protected abstract V toValueType(String value);

    protected abstract String getName();

    public V get(String key) {
        return cache.get(key);
    }

    public void put(String key, V apps) {
        cache.put(key, apps);
    }

    public V remove(String key) {
        return cache.remove(key);
    }

    public Map<String, V> getAll() {
        if (cache.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, V> copyMap = new HashMap<>();
        cache.lock();
        try {
            for (Map.Entry<String, V> entry : cache.entrySet()) {
                copyMap.put(entry.getKey(), entry.getValue());
            }
        } finally {
            cache.releaseLock();
        }
        return Collections.unmodifiableMap(copyMap);
    }

    public void update(Map<String, V> newCache) {
        for (Map.Entry<String, V> entry : newCache.entrySet()) {
            cache.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void destroy() {
        // destroy in FILO order.
        Disposable[] elements = this.disposableResources.toArray(new Disposable[0]);

        for (int i = elements.length - 1; i >= 0; i--) {
            elements[i].destroy();
        }
        this.disposableResources.clear();
    }

    public static class CacheRefreshTask<V> implements Runnable {
        private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
        private static final String DEFAULT_COMMENT = "Dubbo cache";
        private final FileCacheStore cacheStore;
        private final LRUCache<String, V> cache;
        private final AbstractCacheManager<V> cacheManager;
        private final long maxFileSize;

        public CacheRefreshTask(
                FileCacheStore cacheStore,
                LRUCache<String, V> cache,
                AbstractCacheManager<V> cacheManager,
                long maxFileSize) {
            this.cacheStore = cacheStore;
            this.cache = cache;
            this.cacheManager = cacheManager;
            this.maxFileSize = maxFileSize;
        }

        @Override
        public void run() {
            Map<String, String> properties = new HashMap<>();

            cache.lock();
            try {
                for (Map.Entry<String, V> entry : cache.entrySet()) {
                    properties.put(entry.getKey(), JsonUtils.toJson(entry.getValue()));
                }
            } finally {
                cache.releaseLock();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Dumping " + cacheManager.getName() + " caches, latest entries " + properties.size());
            }
            cacheStore.refreshCache(properties, DEFAULT_COMMENT, maxFileSize);
        }
    }

    // for test unit
    public FileCacheStore getCacheStore() {
        return cacheStore;
    }

    public LRUCache<String, V> getCache() {
        return cache;
    }
}
