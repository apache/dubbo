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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi.model;

import org.apache.dubbo.common.utils.ToStringUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Node<T extends Node<T>> implements Cloneable {

    private Map<String, Object> extensions;

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @SuppressWarnings("unchecked")
    public T addExtension(String name, Object value) {
        Map<String, Object> extensions = this.extensions;
        if (extensions == null) {
            this.extensions = extensions = new LinkedHashMap<>();
        }
        extensions.put(name, value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addExtensions(Map<String, ?> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return (T) this;
        }

        Map<String, Object> thisExtensions = this.extensions;
        if (thisExtensions == null) {
            this.extensions = new LinkedHashMap<>(extensions);
        } else {
            for (Map.Entry<String, ?> entry : extensions.entrySet()) {
                thisExtensions.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return (T) this;
    }

    public void removeExtension(String name) {
        if (extensions != null) {
            extensions.remove(name);
        }
    }

    @SuppressWarnings("unchecked")
    public T setExtensions(Map<String, ?> extensions) {
        if (extensions != null) {
            this.extensions = new LinkedHashMap<>(extensions);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T clone() {
        try {
            T clone = (T) super.clone();
            if (extensions != null) {
                clone.setExtensions(extensions);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public String toString() {
        return ToStringUtils.printToString(this);
    }

    public static <T extends Node<T>> T clone(T node) {
        return node == null ? null : node.clone();
    }

    public static <T extends Node<T>> List<T> clone(List<T> list) {
        if (list == null) {
            return null;
        }
        int size = list.size();
        if (size == 0) {
            return new ArrayList<>();
        }
        List<T> clone = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            clone.add(list.get(i).clone());
        }
        return clone;
    }

    public static <K, V extends Node<V>> Map<K, V> clone(Map<K, V> map) {
        if (map == null) {
            return null;
        }
        int size = map.size();
        if (size == 0) {
            return new LinkedHashMap<>();
        }
        Map<K, V> clone = newMap(size);
        for (Map.Entry<K, V> entry : map.entrySet()) {
            clone.put(entry.getKey(), entry.getValue().clone());
        }
        return clone;
    }

    protected static void write(Map<String, Object> node, String name, Object value) {
        if (value == null || "".equals(value)) {
            return;
        }
        node.put(name, value instanceof Set ? ((Set<?>) value).toArray() : value);
    }

    protected static void write(Map<String, Object> node, String name, Node<?> value, Context context) {
        if (value == null) {
            return;
        }
        Map<String, Object> valueMap = value.writeTo(new LinkedHashMap<>(), context);
        if (valueMap == null || valueMap.isEmpty()) {
            return;
        }
        node.put(name, valueMap);
    }

    protected static void write(Map<String, Object> node, String name, List<? extends Node<?>> value, Context context) {
        if (value == null) {
            return;
        }
        int size = value.size();
        if (size > 0) {
            List<Map<String, Object>> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Map<String, Object> valueMap = value.get(i).writeTo(new LinkedHashMap<>(), context);
                if (valueMap == null || valueMap.isEmpty()) {
                    continue;
                }
                list.add(valueMap);
            }
            node.put(name, list);
        }
    }

    protected static void write(
            Map<String, Object> node, String name, Map<?, ? extends Node<?>> value, Context context) {
        if (value == null) {
            return;
        }
        int size = value.size();
        if (size > 0) {
            Map<Object, Map<String, Object>> map = newMap(size);
            for (Map.Entry<?, ? extends Node<?>> entry : value.entrySet()) {
                Map<String, Object> valueMap = entry.getValue().writeTo(new LinkedHashMap<>(), context);
                if (valueMap == null || valueMap.isEmpty()) {
                    continue;
                }
                map.put(entry.getKey(), valueMap);
            }
            node.put(name, map);
        }
    }

    protected static <K, V> Map<K, V> newMap(int capacity) {
        return new LinkedHashMap<>(capacity < 3 ? capacity + 1 : (int) (capacity / 0.75F + 1.0F));
    }

    protected final void writeExtensions(Map<String, Object> node) {
        if (extensions == null) {
            return;
        }
        node.putAll(extensions);
    }

    public abstract Map<String, Object> writeTo(Map<String, Object> node, Context context);
}
