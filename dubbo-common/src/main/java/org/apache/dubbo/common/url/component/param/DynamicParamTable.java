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

import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Global Param Cache Table
 * Not support method parameters
 */
public final class DynamicParamTable {
    /**
     * Keys array, value is key's identity hashcode ( assume key is in constant pool )
     */
    private static int[] KEYS;
    /**
     * Keys array, value is string
     */
    private static String[] ORIGIN_KEYS;
    private static ParamValue[] VALUES;
    private static final Map<String, Integer> KEY2INDEX = new HashMap<>(64);

    private DynamicParamTable() {
        throw new IllegalStateException();
    }

    static {
        init();
    }

    public static int getKeyIndex(boolean enabled, String key) {
        if (!enabled) {
            return -1;
        }
        // assume key is in constant pool
        int identityHashCode = System.identityHashCode(key);
        int index = Arrays.binarySearch(KEYS, identityHashCode);
        if (index >= 0) {
            return index;
        }
        // fallback to key2index map
        Integer indexFromMap = KEY2INDEX.get(key);
        return indexFromMap == null ? -1 : indexFromMap;
    }

    public static int getValueIndex(String key, String value) {
        int idx = getKeyIndex(true, key);
        if (idx < 0) {
            throw new IllegalArgumentException("Cannot found key in url param:" + key);
        }
        ParamValue paramValue = VALUES[idx];
        return paramValue.getIndex(value);
    }

    public static String getKey(int offset) {
        return ORIGIN_KEYS[offset];
    }

    public static String getValue(int vi, int offset) {
        return VALUES[vi].getN(offset);
    }

    private static void init() {
        List<String> keys = new LinkedList<>();
        List<ParamValue> values = new LinkedList<>();
        Map<String, Integer> key2Index = new HashMap<>(64);
        keys.add("");
        values.add(new DynamicValues(null));

        FrameworkModel.defaultModel().getExtensionLoader(DynamicParamSource.class)
            .getSupportedExtensionInstances().forEach(source -> source.init(keys, values));

        TreeMap<String, ParamValue> resultMap = new TreeMap<>(Comparator.comparingInt(System::identityHashCode));
        for (int i = 0; i < keys.size(); i++) {
            resultMap.put(keys.get(i), values.get(i));
        }

        // assume key is in constant pool, store identity hashCode as index
        KEYS = resultMap.keySet()
            .stream()
            .map(System::identityHashCode)
            .mapToInt(x -> x)
            .toArray();

        ORIGIN_KEYS = resultMap.keySet().toArray(new String[0]);

        VALUES = resultMap.values().toArray(new ParamValue[0]);

        for (int i = 0; i < ORIGIN_KEYS.length; i++) {
            key2Index.put(ORIGIN_KEYS[i], i);
        }
        KEY2INDEX.putAll(key2Index);
    }
}
