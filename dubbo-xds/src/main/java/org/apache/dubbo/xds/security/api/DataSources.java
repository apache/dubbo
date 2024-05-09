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
package org.apache.dubbo.xds.security.api;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.common.utils.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.envoyproxy.envoy.config.core.v3.DataSource;

public enum DataSources {

    /**
     * this DataSource represents a file path
     */
    LOCAL_FILE,

    /**
     * this DataSource represents an environment variable
     */
    ENVIRONMENT_VARIABLE,

    /**
     * this DataSource represents an inline string
     */
    INLINE_STRING,

    /**
     * this DataSource represents inline bytes
     */
    INLINE_BYTES;

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DataSources.class);

    public static Pair<String, DataSources> resolveDataSource(DataSource dataSource) {
        if (dataSource.hasFilename()) {
            return new Pair<>(dataSource.getFilename(), LOCAL_FILE);
        }
        if (dataSource.hasEnvironmentVariable()) {
            return new Pair<>(dataSource.getEnvironmentVariable(), ENVIRONMENT_VARIABLE);
        }
        if (dataSource.hasInlineString()) {
            return new Pair<>(dataSource.getInlineString(), INLINE_STRING);
        }
        if (dataSource.hasInlineBytes()) {
            return new Pair<>(dataSource.getInlineBytes().toStringUtf8(), INLINE_BYTES);
        }
        throw new IllegalArgumentException("Unknown data source type");
    }

    public static String readActualValue(Pair<String, DataSources> dataSource) {
        return readActualValue(dataSource, null);
    }

    public static String readActualValue(Pair<String, DataSources> dataSource, FileWatcher watcher) {
        switch (dataSource.getValue()) {
            case LOCAL_FILE:
                if (watcher != null) {
                    String value = new String(watcher.readWatchedFile(dataSource.getKey()));
                    if (StringUtils.isEmpty(value)) {
                        try {
                            watcher.registerWatch(dataSource.getKey());
                            return new String(watcher.readWatchedFile(dataSource.getKey()));
                        } catch (Exception e) {
                            logger.warn("99-1", "", "", "Failed to register watch for file: " + dataSource.getKey(), e);
                        }
                    }
                }
                try {
                    return IOUtils.read(
                            Files.newInputStream(Paths.get(dataSource.getKey())), StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    logger.error("99-1", "", "", "Failed to read file: " + dataSource.getKey(), e);
                    return null;
                }
            case ENVIRONMENT_VARIABLE:
                return System.getenv(dataSource.getKey());
            case INLINE_STRING:
            case INLINE_BYTES:
                // bytes were read as UTF-8 string
                return dataSource.getKey();
            default:
                throw new IllegalArgumentException("Unknown data source type");
        }
    }

    public static String readActualValue(DataSource dataSource, FileWatcher watcher) {
        return readActualValue(resolveDataSource(dataSource), watcher);
    }

    public static String readActualValue(DataSource dataSource) {
        return readActualValue(resolveDataSource(dataSource));
    }
}
