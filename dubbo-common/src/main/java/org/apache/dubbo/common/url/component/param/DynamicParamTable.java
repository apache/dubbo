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
package org.apache.dubbo.common.url.component.param;

import org.apache.dubbo.common.extension.ExtensionLoader;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global Param Cache Table
 * Not support method parameters
 */
public final class DynamicParamTable {
    private static final List<String> KEYS = new CopyOnWriteArrayList<>();
    private static final List<ParamValue> VALUES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> KEY2INDEX = new HashMap<>(64);

    private DynamicParamTable() {
        throw new IllegalStateException();
    }

    static {
        init();
    }

    public static Integer getKeyIndex(boolean enabled, String key) {
        if (!enabled) {
            return null;
        }
        return KEY2INDEX.get(key);
    }

    public static Integer getValueIndex(String key, String value) {
        Integer idx = getKeyIndex(true, key);
        if (idx == null) {
            throw new IllegalArgumentException("Cannot found key in url param:" + key);
        }
        ParamValue paramValue = VALUES.get(idx);
        return paramValue.getIndex(value);
    }

    public static String getKey(int offset) {
        return KEYS.get(offset);
    }

    public static boolean isDefaultValue(String key, String value) {
        return Objects.equals(value, VALUES.get(getKeyIndex(true, key)).defaultVal());
    }

    public static String getValue(int vi, Integer offset) {
        return VALUES.get(vi).getN(offset);
    }

    public static String getDefaultValue(int vi) {
        return VALUES.get(vi).defaultVal();
    }

    private static void init() {
        List<String> keys = new LinkedList<>();
        List<ParamValue> values = new LinkedList<>();
        Map<String, Integer> key2Index = new HashMap<>(64);
        keys.add("");
        values.add(new DynamicValues(null));

        ExtensionLoader.getExtensionLoader(DynamicParamSource.class)
                .getSupportedExtensionInstances().forEach(source -> source.init(keys, values));

        for (int i = 0; i < keys.size(); i++) {
            if (!KEYS.contains(keys.get(i))) {
                KEYS.add(keys.get(i));
                VALUES.add(values.get(i));
            }
        }

        for (int i = 0; i < KEYS.size(); i++) {
            if (!KEYS.get(i).isEmpty()) {
                key2Index.put(KEYS.get(i), i);
            }
        }
        KEY2INDEX.putAll(key2Index);
    }
}
