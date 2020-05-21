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
package org.apache.dubbo.rpc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class provides map adapters to support attachments in RpcContext, Invocation and Result switch from
 * <String, String> to <String, Object>
 */
public class AttachmentsAdapter {

    public static class ObjectToStringMap implements Map<String, String> {

        private Map<String, String> attachments;

        public ObjectToStringMap(Map<String, Object> attachments) {
            Map<String, String> tmpMap = new HashMap<>();
            for (Entry<String, Object> entry : attachments.entrySet()) {
                tmpMap.put(entry.getKey(), convert(entry.getValue()));
            }
            this.attachments = tmpMap;
        }

        private String convert(Object obj) {
            if (obj instanceof String) {
                return (String) obj;
            }
            return null; // or JSON.toString(obj);
        }

        @Override
        public int size() {
            return attachments.size();
        }

        @Override
        public boolean isEmpty() {
            return attachments.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return attachments.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return attachments.containsValue(value);
        }

        @Override
        public String get(Object key) {
            return attachments.get(key);
        }

        @Override
        public String put(String key, String value) {
            return attachments.put(key, value);
        }

        @Override
        public String remove(Object key) {
            return attachments.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            attachments.putAll(m);
        }

        @Override
        public void clear() {
            attachments.clear();
        }

        @Override
        public Set<String> keySet() {
            return attachments.keySet();
        }

        @Override
        public Collection<String> values() {
            return attachments.values();
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return attachments.entrySet();
        }

        @Override
        public boolean equals(Object o) {
            return attachments.equals(o);
        }

        @Override
        public int hashCode() {
            return attachments.hashCode();
        }
    }


    public static class StringToObjectMap implements Map<String, Object> {
        private Map<String, Object> attachments;

        public StringToObjectMap(Map<String, String> attachments) {
            Map<String, Object> tmpMap = new HashMap<>();
            for (Entry<String, String> entry : attachments.entrySet()) {
                tmpMap.put(entry.getKey(), entry.getValue());
            }
            this.attachments = tmpMap;
        }

        @Override
        public int size() {
            return attachments.size();
        }

        @Override
        public boolean isEmpty() {
            return attachments.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return attachments.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return attachments.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return attachments.get(key);
        }

        @Override
        public Object put(String key, Object value) {
            return attachments.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return attachments.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> m) {
            attachments.putAll(m);
        }

        @Override
        public void clear() {
            attachments.clear();
        }

        @Override
        public Set<String> keySet() {
            return attachments.keySet();
        }

        @Override
        public Collection<Object> values() {
            return attachments.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return attachments.entrySet();
        }

        @Override
        public boolean equals(Object o) {
            return attachments.equals(o);
        }

        @Override
        public int hashCode() {
            return attachments.hashCode();
        }
    }
}
