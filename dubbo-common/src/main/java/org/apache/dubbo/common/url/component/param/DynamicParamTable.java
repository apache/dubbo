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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

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

    public static Integer getKeyIndex(boolean enabled, String key) {
        if (!enabled) {
            return null;
        }
        // assume key is in constant pool
        int identityHashCode = System.identityHashCode(key);
        int index = Arrays.binarySearch(KEYS, identityHashCode);
        if (index >= 0) {
            return index;
        }
        return KEY2INDEX.get(key);
    }

    public static Integer getValueIndex(String key, String value) {
        Integer idx = getKeyIndex(true, key);
        if (idx == null) {
            throw new IllegalArgumentException("Cannot found key in url param:" + key);
        }
        ParamValue paramValue = VALUES[idx];
        return paramValue.getIndex(value);
    }

    public static String getKey(int offset) {
        return ORIGIN_KEYS[offset];
    }

    public static boolean isDefaultValue(String key, String value) {
        return Objects.equals(value, VALUES[getKeyIndex(true, key)].defaultVal());
    }

    public static String getValue(int vi, Integer offset) {
        return VALUES[vi].getN(offset);
    }

    public static String getDefaultValue(int vi) {
        return VALUES[vi].defaultVal();
    }

    private static void init() {
        List<String> keys = new LinkedList<>();
        List<ParamValue> values = new LinkedList<>();
        Map<String, Integer> key2Index = new HashMap<>(64);
        keys.add("");
        values.add(new DynamicValues(null));

        ExtensionLoader.getExtensionLoader(DynamicParamSource.class)
                .getSupportedExtensionInstances().forEach(source -> source.init(keys, values));

        List<Pair<String, ParamValue>> resultList = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            if (!resultList.contains(new Pair<>(keys.get(i), null))) {
                resultList.add(new Pair<>(keys.get(i), values.get(i)));
            }
        }

        // assume key is in constant pool, store identity hashCode as index
        resultList.sort(Comparator.comparingInt(a -> System.identityHashCode(a.getKey())));
        KEYS = resultList.stream()
            .map(Pair::getKey)
            .map(System::identityHashCode)
            .mapToInt(x -> x)
            .toArray();

        ORIGIN_KEYS = resultList.stream()
            .map(Pair::getKey)
            .toArray(String[]::new);

        VALUES = resultList.stream()
            .map(Pair::getValue)
            .toArray(ParamValue[]::new);

        for (int i = 0; i < resultList.size(); i++) {
            if (!resultList.get(i).getKey().isEmpty()) {
                key2Index.put(resultList.get(i).getKey(), i);
            }
        }
        KEY2INDEX.putAll(key2Index);
    }

    private static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(key, pair.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
