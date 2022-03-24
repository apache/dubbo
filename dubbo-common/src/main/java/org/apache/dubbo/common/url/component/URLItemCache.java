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
package org.apache.dubbo.common.url.component;

import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Map;

public class URLItemCache {
    // thread safe with limited size, by default 1000
    private static final Map<String, String> PARAM_KEY_CACHE = new LRUCache<>(10000);
    private static final Map<String, String> PARAM_VALUE_CACHE = new LRUCache<>(50000);
    private static final Map<String, String> PATH_CACHE = new LRUCache<>(10000);
    private static final Map<String, String> REVISION_CACHE = new LRUCache<>(10000);

    public static void putParams(Map<String, String> params, String key, String value) {
        String cachedKey = PARAM_KEY_CACHE.get(key);
        if (StringUtils.isBlank(cachedKey)) {
            cachedKey = key;
            PARAM_KEY_CACHE.put(key, key);
        }
        String cachedValue = PARAM_VALUE_CACHE.get(value);
        if (StringUtils.isBlank(cachedValue)) {
            cachedValue = value;
            PARAM_VALUE_CACHE.put(value, value);
        }

        params.put(cachedKey, cachedValue);
    }

    public static String checkPath(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        String cachedPath = PATH_CACHE.putIfAbsent(path, path);
        if (StringUtils.isNotBlank(cachedPath)) {
            return cachedPath;
        }
        return path;
    }

    public static String checkRevision(String revision) {
        if (StringUtils.isBlank(revision)) {
            return revision;
        }
        String cachedRevision = REVISION_CACHE.putIfAbsent(revision, revision);
        if (StringUtils.isNotBlank(cachedRevision)) {
            return cachedRevision;
        }
        return revision;
    }

    public static String intern(String protocol) {
        if (StringUtils.isBlank(protocol)) {
            return protocol;
        }
        return protocol.intern();
    }

    public static void putParamsIntern(Map<String, String> params, String key, String value) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            params.put(key, value);
            return;
        }
        key = key.intern();
        value = value.intern();
        params.put(key, value);
    }
}
