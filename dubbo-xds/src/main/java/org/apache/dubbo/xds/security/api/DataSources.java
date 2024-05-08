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

import org.apache.dubbo.common.utils.Pair;

import io.envoyproxy.envoy.config.core.v3.DataSource;

public enum DataSources {
    LOCAL_FILE,
    ENVIRONMENT_VARIABLE,
    INLINE_STRING,
    INLINE_BYTES;

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
        switch (dataSource.getValue()) {
            case LOCAL_FILE:
                return dataSource.getKey();
            case ENVIRONMENT_VARIABLE:
                return System.getenv(dataSource.getKey());
            case INLINE_STRING:
                return dataSource.getKey();
            case INLINE_BYTES:
                return dataSource.getKey();
            default:
                throw new IllegalArgumentException("Unknown data source type");
        }
    }

    public static String readActualValue(DataSource dataSource) {
        return readActualValue(resolveDataSource(dataSource));
    }
}
