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

public class RpcContextMap implements Map<String, Object> {
    private final RpcContextAttachment clientAttachment;
    private final RpcContextAttachment serverAttachment;

    public RpcContextMap(RpcContextAttachment clientAttachment, RpcContextAttachment serverAttachment) {
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
    public Object get(Object key) {
        Object value = clientAttachment.getObjectAttachment((String) key);
        if (value != null) {
            return value;
        }
        return serverAttachment.getObjectAttachment((String)key);
    }

    @Override
    public Object put(String key, Object value) {
        return clientAttachment.setAttachment(key, value);
    }

    @Override
    public Object remove(Object key) {
        serverAttachment.removeAttachment((String) key);
        return clientAttachment.removeAttachment((String) key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
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
    public Collection<Object> values() {
        List<Object> values = new ArrayList<>(clientAttachment.getObjectAttachments().values());
        for (Entry<String, Object> entry : serverAttachment.getObjectAttachments().entrySet()) {
            if (!clientAttachment.getObjectAttachments().containsKey(entry.getKey())) {
                values.add(entry.getValue());
            }
        }
        return values;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<Entry<String, Object>>() {
            @Override
            public Iterator<Entry<String, Object>> iterator() {
                Set<ContextEntry> entrySet = new HashSet<>();
                for (Entry<String, Object> entry : clientAttachment.getObjectAttachments().entrySet()) {
                    entrySet.add(new ContextEntry(entry.getKey(), clientAttachment));
                }
                for (Entry<String, Object> entry : serverAttachment.getObjectAttachments().entrySet()) {
                    if (!clientAttachment.getObjectAttachments().containsKey(entry.getKey())) {
                        entrySet.add(new ContextEntry(entry.getKey(), serverAttachment));
                    }
                }
                Iterator<ContextEntry> realIterator = entrySet.iterator();
                return new Iterator<Entry<String, Object>>() {
                    private volatile ContextEntry current = null;

                    @Override
                    public boolean hasNext() {
                        return realIterator.hasNext();
                    }

                    @Override
                    public Entry<String, Object> next() {
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

    private static class ContextEntry implements Entry<String, Object> {
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
        public Object getValue() {
            return attachment.getObjectAttachment(key);
        }

        @Override
        public Object setValue(Object value) {
            return attachment.setObjectAttachment(key, value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcContextMap that = (RpcContextMap) o;
        return Objects.equals(clientAttachment, that.clientAttachment) && Objects.equals(serverAttachment, that.serverAttachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientAttachment, serverAttachment);
    }
}
