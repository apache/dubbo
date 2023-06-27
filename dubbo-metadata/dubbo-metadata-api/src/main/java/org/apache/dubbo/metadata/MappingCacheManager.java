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

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/**
 * TODO, Using randomly accessible file-based cache can be another choice if memory consumption turns to be an issue.
 */
public class MappingCacheManager extends AbstractCacheManager<Set<String>> {
    private static final String DEFAULT_FILE_NAME = ".mapping";
    private static final int DEFAULT_ENTRY_SIZE = 10000;

    public static MappingCacheManager getInstance(ScopeModel scopeModel) {
        return scopeModel.getBeanFactory().getOrRegisterBean(MappingCacheManager.class);
    }

    public MappingCacheManager(boolean enableFileCache, String name, ScheduledExecutorService executorService) {
        String filePath = System.getProperty("dubbo.mapping.cache.filePath");
        String fileName = System.getProperty("dubbo.mapping.cache.fileName");
        if (StringUtils.isEmpty(fileName)) {
            fileName = DEFAULT_FILE_NAME;
        }

        if (StringUtils.isNotEmpty(name)) {
            fileName = fileName + "." + name;
        }

        String rawEntrySize = System.getProperty("dubbo.mapping.cache.entrySize");
        int entrySize = StringUtils.parseInteger(rawEntrySize);
        entrySize = (entrySize == 0 ? DEFAULT_ENTRY_SIZE : entrySize);

        String rawMaxFileSize = System.getProperty("dubbo.mapping.cache.maxFileSize");
        long maxFileSize = StringUtils.parseLong(rawMaxFileSize);

        init(enableFileCache, filePath, fileName, entrySize,  maxFileSize, 50, executorService);
    }

    @Override
    protected Set<String> toValueType(String value) {
        return new HashSet<>(JsonUtils.toJavaList(value, String.class));
    }

    @Override
    protected String getName() {
        return "mapping";
    }
}
