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

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.AbstractCacheManager;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Metadata cache with limited size that uses LRU expiry policy.
 */
public class MetaCacheManager extends AbstractCacheManager<MetadataInfo> {
    private static final String DEFAULT_FILE_NAME = ".metadata";
    private static final int DEFAULT_ENTRY_SIZE = 100;

    public static MetaCacheManager getInstance(ScopeModel scopeModel) {
        return scopeModel.getBeanFactory().getOrRegisterBean(MetaCacheManager.class);
    }

    public MetaCacheManager(boolean enableFileCache, String registryName, ScheduledExecutorService executorService) {
        String filePath = System.getProperty("dubbo.meta.cache.filePath");
        String fileName = System.getProperty("dubbo.meta.cache.fileName");
        if (StringUtils.isEmpty(fileName)) {
            fileName = DEFAULT_FILE_NAME;
        }

        if (StringUtils.isNotEmpty(registryName)) {
            fileName = fileName + "." + registryName;
        }

        String rawEntrySize = System.getProperty("dubbo.meta.cache.entrySize");
        int entrySize = StringUtils.parseInteger(rawEntrySize);
        entrySize = (entrySize == 0 ? DEFAULT_ENTRY_SIZE : entrySize);

        String rawMaxFileSize = System.getProperty("dubbo.meta.cache.maxFileSize");
        long maxFileSize = StringUtils.parseLong(rawMaxFileSize);

        init(enableFileCache, filePath, fileName, entrySize, maxFileSize, 60, executorService);
    }

    // for unit test only
    public MetaCacheManager() {
        this(true, "", null);
    }

    @Override
    protected MetadataInfo toValueType(String value) {
        return JsonUtils.toJavaObject(value, MetadataInfo.class);
    }

    @Override
    protected String getName() {
        return "meta";
    }
}
