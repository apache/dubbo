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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class URLItemCache {
    // thread safe with limited size, by default 1000
    private static final Map<String, String> PARAM_KEY_CACHE = new LRUCache<>(10000);
    private static final Map<String, String> PARAM_VALUE_CACHE = new LRUCache<>(100000);
    private static final Map<String, String> PATH_CACHE = new LRUCache<>(10000);
    private static final Map<String, String> PROTOCOL_CACHE = new ConcurrentHashMap<>();

    public static void putParams(Map<String, String> params, String key, String value) {
        String cachedKey = PARAM_KEY_CACHE.get(key);
        if (cachedKey == null) {
            cachedKey = key;
            PARAM_KEY_CACHE.put(key, key);
        }
        String cachedValue = PARAM_VALUE_CACHE.get(value);
        if (cachedValue == null) {
            cachedValue = value;
            PARAM_VALUE_CACHE.put(value, value);
        }

        params.put(cachedKey, cachedValue);
    }

    public static String checkProtocol(String _protocol) {
        if (_protocol == null) {
            return _protocol;
        }
        String cachedProtocol = PROTOCOL_CACHE.putIfAbsent(_protocol, _protocol);
        if (cachedProtocol != null) {
            return cachedProtocol;
        }
        return _protocol;
    }

    public static String checkPath(String _path) {
        if (_path == null) {
            return _path;
        }
        String cachedPath = PATH_CACHE.putIfAbsent(_path, _path);
        if (cachedPath != null) {
            return cachedPath;
        }
        return _path;
    }
}
