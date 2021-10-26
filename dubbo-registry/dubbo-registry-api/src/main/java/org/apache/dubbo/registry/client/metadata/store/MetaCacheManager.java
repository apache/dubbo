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
package org.apache.dubbo.registry.client.metadata.store;

import org.apache.dubbo.common.cache.FileCacheStore;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Metadata cache with limited size that uses LRU expiry policy.
 */
public class MetaCacheManager implements ScopeModelAware {
    private static final Logger logger = LoggerFactory.getLogger(MetaCacheManager.class);
    private static final String DEFAULT_FILE_NAME = ".metadata";
    private static final String SUFFIX = ".dubbo.cache";

    private static final long delay = 1L;
    private ScheduledExecutorService executorService;

    protected FileCacheStore cacheStore;
    protected LRUCache<String, MetadataInfo> cache = new LRUCache<>(10000);

    public MetaCacheManager() {
        String filePath = System.getProperty("dubbo.meta.cache.filePath");
        String fileName = System.getProperty("dubbo.meta.cache.fileName");
        if (StringUtils.isEmpty(fileName)) {
            fileName = DEFAULT_FILE_NAME;
        }
        fileName += SUFFIX;
        try {
            cacheStore = new FileCacheStore(filePath, fileName);
            Properties properties = cacheStore.loadCache();
            logger.info("Successfully loaded meta cache from file " + fileName + ", entries " + properties.size());
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                MetadataInfo metadataInfo = JsonUtils.getGson().fromJson(value, MetadataInfo.class);
                cache.put(key, metadataInfo);
            }

            executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-cache-refresh", true));
            executorService.scheduleWithFixedDelay(new CacheRefreshTask(cacheStore, cache), delay, delay, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.error("Load metadata from local cache file error ", e);
        }
    }

    public MetadataInfo get(String key) {
        return cache.get(key);
    }

    public void put(String key, MetadataInfo metadataInfo) {
        cache.put(key, metadataInfo);
    }

    public Map<String, MetadataInfo> getAll() {
        if (cache.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, MetadataInfo> copyMap = new HashMap<>();
        cache.lock();
        try {
            for (Map.Entry<String, MetadataInfo> entry : cache.entrySet()) {
                copyMap.put(entry.getKey(), entry.getValue());
            }
        } finally {
            cache.releaseLock();
        }
        return Collections.unmodifiableMap(copyMap);
    }

    public void update(Map<String, MetadataInfo> revisionToMetadata) {
        for (Map.Entry<String, MetadataInfo> entry : revisionToMetadata.entrySet()) {
            cache.put(entry.getKey(), entry.getValue());
        }
    }

    public void destroy() {
        executorService.shutdown();
    }

    protected static class CacheRefreshTask implements Runnable {
        private final FileCacheStore cacheStore;
        private final LRUCache<String, MetadataInfo> cache;

        public CacheRefreshTask(FileCacheStore cacheStore, LRUCache<String, MetadataInfo> cache) {
            this.cacheStore = cacheStore;
            this.cache = cache;
        }

        @Override
        public void run() {
            Properties properties = new Properties();

            cache.lock();
            try {
                for (Map.Entry<String, MetadataInfo> entry : cache.entrySet()) {
                    properties.put(entry.getKey(), JsonUtils.getGson().toJson(entry.getValue()));
                }
            } finally {
                cache.releaseLock();
            }

            logger.info("Dumping meta caches, latest entries " + properties.size());
            cacheStore.refreshCache(properties);
        }
    }
}
