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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RpcContextStringMap implements Map<String, String> {
    private final RpcContextAttachment clientAttachment;
    private final RpcContextAttachment serverAttachment;

    public RpcContextStringMap(RpcContextAttachment clientAttachment, RpcContextAttachment serverAttachment) {
        this.clientAttachment = clientAttachment;
        this.serverAttachment = serverAttachment;
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return clientAttachment.getObjectAttachments().isEmpty() && serverAttachment.getObjectAttachments().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return clientAttachment.getObjectAttachments().containsKey(key) || serverAttachment.getObjectAttachments().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return clientAttachment.getObjectAttachments().containsValue(value) || serverAttachment.getObjectAttachments().containsValue(value);
    }

    @Override
    public String get(Object key) {
        String value = clientAttachment.getAttachment((String) key);
        if (value != null) {
            return value;
        }
        return serverAttachment.getAttachment((String) key);
    }

    @Override
    public String put(String key, String value) {
        clientAttachment.setAttachment(key, value);
        return null;
    }

    @Override
    public String remove(Object key) {
        serverAttachment.removeAttachment((String) key);
        clientAttachment.removeAttachment((String) key);
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        clientAttachment.getObjectAttachments().putAll(m);
    }

    @Override
    public void clear() {
        clientAttachment.clearAttachments();
        serverAttachment.clearAttachments();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keySet = new HashSet<>(clientAttachment.getObjectAttachments().keySet());
        keySet.addAll(serverAttachment.getObjectAttachments().keySet());
        return keySet;
    }

    @Override
    public Collection<String> values() {
        List<String> values = new ArrayList<>();
        for (Object value : clientAttachment.getObjectAttachments().values()) {
            if (value instanceof String) {
                values.add((String) value);
            }
        }
        for (Entry<String, Object> entry : serverAttachment.getObjectAttachments().entrySet()) {
            if (!clientAttachment.getObjectAttachments().containsKey(entry.getKey()) && entry.getValue() instanceof String) {
                values.add((String) entry.getValue());
            }
        }
        return values;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<Entry<String, String>>() {
            @Override
            public Iterator<Entry<String, String>> iterator() {
                Set<ContextEntry> entrySet = new HashSet<>();
                for (Entry<String, Object> entry : clientAttachment.getObjectAttachments().entrySet()) {
                    if (entry.getValue() instanceof String) {
                        entrySet.add(new ContextEntry(entry.getKey(), clientAttachment));
                    }
                }
                for (Entry<String, Object> entry : serverAttachment.getObjectAttachments().entrySet()) {
                    if (!clientAttachment.getObjectAttachments().containsKey(entry.getKey()) && entry.getValue() instanceof String) {
                        entrySet.add(new ContextEntry(entry.getKey(), serverAttachment));
                    }
                }
                Iterator<ContextEntry> realIterator = entrySet.iterator();
                return new Iterator<Entry<String, String>>() {
                    private volatile ContextEntry current = null;

                    @Override
                    public boolean hasNext() {
                        return realIterator.hasNext();
                    }

                    @Override
                    public Entry<String, String> next() {
                        current = realIterator.next();
                        return current;
                    }

                    @Override
                    public void remove() {
                        clientAttachment.removeAttachment(current.getKey());
                        serverAttachment.removeAttachment(current.getKey());
                    }
                };
            }

            @Override
            public int size() {
                return keySet().size();
            }
        };
    }

    private static class ContextEntry implements Entry<String, String> {
        private final String key;
        private final RpcContextAttachment attachment;

        public ContextEntry(String key, RpcContextAttachment attachment) {
            this.key = key;
            this.attachment = attachment;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return attachment.getAttachment(key);
        }

        @Override
        public String setValue(String value) {
            attachment.setAttachment(key, value);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcContextStringMap that = (RpcContextStringMap) o;
        return Objects.equals(clientAttachment, that.clientAttachment) && Objects.equals(serverAttachment, that.serverAttachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientAttachment, serverAttachment);
    }
}
