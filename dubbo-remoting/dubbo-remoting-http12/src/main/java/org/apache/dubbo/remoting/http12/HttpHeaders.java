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
package org.apache.dubbo.remoting.http12;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class HttpHeaders implements Map<String, List<String>>, Serializable, Cloneable {

    private final LinkedHashMap<String, List<String>> targetMap;

    private final HashMap<String, String> caseInsensitiveKeys;

    private final Locale locale;

    private transient volatile Set<String> keySet;

    private transient volatile Collection<List<String>> values;

    private transient volatile Set<Entry<String, List<String>>> entrySet;

    public HttpHeaders() {
        this.targetMap = new LinkedHashMap<>();
        this.caseInsensitiveKeys = new HashMap<>();
        this.locale = Locale.US;
    }


    // Implementation of java.util.Map

    @Override
    public int size() {
        return this.targetMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.targetMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return (key instanceof String && this.caseInsensitiveKeys.containsKey(convertKey((String) key)));
    }

    @Override
    public boolean containsValue(Object value) {
        return this.targetMap.containsValue(value);
    }

    @Override
    public List<String> get(Object key) {
        if (key instanceof String) {
            String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey((String) key));
            if (caseInsensitiveKey != null) {
                return this.targetMap.get(caseInsensitiveKey);
            }
        }
        return null;
    }

    public String getFirst(String name) {
        List<String> values = get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    public void set(String name, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        put(name, values);
    }

    @Override

    public List<String> getOrDefault(Object key, List<String> defaultValue) {
        if (key instanceof String) {
            String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey((String) key));
            if (caseInsensitiveKey != null) {
                return this.targetMap.get(caseInsensitiveKey);
            }
        }
        return defaultValue;
    }

    @Override
    public List<String> put(String key, List<String> value) {
        String oldKey = this.caseInsensitiveKeys.put(convertKey(key), key);
        List<String> oldKeyValue = null;
        if (oldKey != null && !oldKey.equals(key)) {
            oldKeyValue = this.targetMap.remove(oldKey);
        }
        List<String> oldValue = this.targetMap.put(key, value);
        return (oldKeyValue != null ? oldKeyValue : oldValue);
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
        if (map.isEmpty()) {
            return;
        }
        map.forEach(this::put);
    }

    @Override

    public List<String> putIfAbsent(String key, List<String> value) {
        String oldKey = this.caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (oldKey != null) {
            List<String> oldKeyValue = this.targetMap.get(oldKey);
            if (oldKeyValue != null) {
                return oldKeyValue;
            } else {
                key = oldKey;
            }
        }
        return this.targetMap.putIfAbsent(key, value);
    }

    @Override

    public List<String> computeIfAbsent(String key, Function<? super String, ? extends List<String>> mappingFunction) {
        String oldKey = this.caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (oldKey != null) {
            List<String> oldKeyValue = this.targetMap.get(oldKey);
            if (oldKeyValue != null) {
                return oldKeyValue;
            } else {
                key = oldKey;
            }
        }
        return this.targetMap.computeIfAbsent(key, mappingFunction);
    }

    @Override

    public List<String> remove(Object key) {
        if (key instanceof String) {
            String caseInsensitiveKey = removeCaseInsensitiveKey((String) key);
            if (caseInsensitiveKey != null) {
                return this.targetMap.remove(caseInsensitiveKey);
            }
        }
        return null;
    }

    @Override
    public void clear() {
        this.caseInsensitiveKeys.clear();
        this.targetMap.clear();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keySet = this.keySet;
        if (keySet == null) {
            keySet = new KeySet(this.targetMap.keySet());
            this.keySet = keySet;
        }
        return keySet;
    }

    @Override
    public Collection<List<String>> values() {
        Collection<List<String>> values = this.values;
        if (values == null) {
            values = new Values(this.targetMap.values());
            this.values = values;
        }
        return values;
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        Set<Entry<String, List<String>>> entrySet = this.entrySet;
        if (entrySet == null) {
            entrySet = new EntrySet(this.targetMap.entrySet());
            this.entrySet = entrySet;
        }
        return entrySet;
    }

    public Map<String, String> toSingleValueMap() {
        Map<String, String> result = new HashMap<>(this.targetMap.size());
        for (String key : keySet()) {
            result.put(key, getFirst(key));
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || this.targetMap.equals(other));
    }

    @Override
    public int hashCode() {
        return this.targetMap.hashCode();
    }

    @Override
    public String toString() {
        return this.targetMap.toString();
    }


    public Locale getLocale() {
        return this.locale;
    }

    protected String convertKey(String key) {
        return key.toLowerCase(getLocale());
    }

    protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
        return false;
    }


    private String removeCaseInsensitiveKey(String key) {
        return this.caseInsensitiveKeys.remove(convertKey(key));
    }


    private class KeySet extends AbstractSet<String> {

        private final Set<String> delegate;

        KeySet(Set<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<String> iterator() {
            return new KeySetIterator();
        }

        @Override
        public boolean remove(Object o) {
            return HttpHeaders.this.remove(o) != null;
        }

        @Override
        public void clear() {
            HttpHeaders.this.clear();
        }

        @Override
        public Spliterator<String> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super String> action) {
            this.delegate.forEach(action);
        }
    }


    private class Values extends AbstractCollection<List<String>> {

        private final Collection<List<String>> delegate;

        Values(Collection<List<String>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<List<String>> iterator() {
            return new ValuesIterator();
        }

        @Override
        public void clear() {
            HttpHeaders.this.clear();
        }

        @Override
        public Spliterator<List<String>> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super List<String>> action) {
            this.delegate.forEach(action);
        }
    }


    private class EntrySet extends AbstractSet<Entry<String, List<String>>> {

        private final Set<Entry<String, List<String>>> delegate;

        public EntrySet(Set<Entry<String, List<String>>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<Entry<String, List<String>>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            if (this.delegate.remove(o)) {
                removeCaseInsensitiveKey(((Map.Entry<String, List<String>>) o).getKey());
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.delegate.clear();
            caseInsensitiveKeys.clear();
        }

        @Override
        public Spliterator<Entry<String, List<String>>> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super Entry<String, List<String>>> action) {
            this.delegate.forEach(action);
        }
    }


    private abstract class EntryIterator<T> implements Iterator<T> {

        private final Iterator<Entry<String, List<String>>> delegate;


        private Entry<String, List<String>> last;

        public EntryIterator() {
            this.delegate = targetMap.entrySet().iterator();
        }

        protected Entry<String, List<String>> nextEntry() {
            Entry<String, List<String>> entry = this.delegate.next();
            this.last = entry;
            return entry;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public void remove() {
            this.delegate.remove();
            if (this.last != null) {
                removeCaseInsensitiveKey(this.last.getKey());
                this.last = null;
            }
        }
    }


    private class KeySetIterator extends EntryIterator<String> {

        @Override
        public String next() {
            return nextEntry().getKey();
        }
    }


    private class ValuesIterator extends EntryIterator<List<String>> {

        @Override
        public List<String> next() {
            return nextEntry().getValue();
        }
    }


    private class EntrySetIterator extends EntryIterator<Entry<String, List<String>>> {

        @Override
        public Entry<String, List<String>> next() {
            return nextEntry();
        }
    }

}
