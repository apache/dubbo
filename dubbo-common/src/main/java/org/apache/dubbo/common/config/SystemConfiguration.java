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


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIXME: is this really necessary? PropertiesConfiguration should have already covered this:
 *
 * @See ConfigUtils#getProperty(String)
 * @see PropertiesConfiguration
 */
public class SystemConfiguration implements Configuration {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public Object getInternalProperty(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        } else {
            Object val = System.getProperty(key);
            if (val != null) {
                cache.putIfAbsent(key, val);
            }
            return val;
        }
    }

    @Override
    public Object getInternalProperty(String key, Object defaultValue) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        } else {
            Object val = System.getProperty(key);
            if (val != null) {
                cache.putIfAbsent(key, val);
            } else {
                val = defaultValue;
                if (defaultValue != null) {
                    cache.putIfAbsent(key, defaultValue);
                }
            }
            return val;
        }
    }

    public void overwriteCache(String key, Object value) {
        if (value != null) {
            cache.put(key, value);
        }
    }

    public void clearCache() {
        cache.clear();
    }



    public Map<String, String> getProperties() {
        return (Map) System.getProperties();
    }
}
