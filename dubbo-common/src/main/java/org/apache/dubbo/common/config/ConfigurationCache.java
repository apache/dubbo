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
package org.apache.dubbo.common.config;

import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Properties Cache of Configuration {@link ConfigurationUtils#getCachedDynamicProperty(ScopeModel, String, String)}
 */
public class ConfigurationCache {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Get Cached Value
     *
     * @param key key
     * @param function function to produce value, should not return `null`
     * @return value
     */
    public String computeIfAbsent(String key, Function<String, String> function) {
        String value = cache.get(key);
        // value might be empty here!
        // empty value from config center will be cached here
        if (value == null) {
            // lock free, tolerate repeat apply, will return previous value
            cache.putIfAbsent(key, function.apply(key));
            value = cache.get(key);
        }
        return value;
    }
}
