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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory configuration
 */
public class InmemoryConfiguration implements Configuration {

    private String name;

    // stores the configuration key-value pairs
    private Map<String, String> store = new LinkedHashMap<>();

    public InmemoryConfiguration() {
    }

    public InmemoryConfiguration(String name) {
        this.name = name;
    }

    public InmemoryConfiguration(Map<String, String> properties) {
        this.setProperties(properties);
    }

    @Override
    public Object getInternalProperty(String key) {
        return store.get(key);
    }

    @Override
    public Object getInternalProperty(String key, Object defaultValue) {
        Object v = store.get(key);
        if (v != null) {
            return v;
        } else {
            return defaultValue;
        }
    }

    /**
     * Add one property into the store, the previous value will be replaced if the key exists
     */
    public void addProperty(String key, String value) {
        store.put(key, value);
    }

    /**
     * Add a set of properties into the store
     */
    public void addProperties(Map<String, String> properties) {
        if (properties != null) {
            this.store.putAll(properties);
        }
    }

    /**
     * set store
     */
    public void setProperties(Map<String, String> properties) {
        if (properties != null) {
            this.store = properties;
        }
    }

    public Map<String, String> getProperties() {
        return store;
    }

}
