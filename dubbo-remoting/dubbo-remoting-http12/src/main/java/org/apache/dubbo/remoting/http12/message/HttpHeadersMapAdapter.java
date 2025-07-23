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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.remoting.http12.HttpHeaders;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class HttpHeadersMapAdapter implements Map<String, List<String>> {

    private final HttpHeaders headers;

    public HttpHeadersMapAdapter(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public int size() {
        return headers.size();
    }

    @Override
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof CharSequence && headers.containsKey((CharSequence) key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (Entry<CharSequence, String> entry : headers) {
            if (Objects.equals(value, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> get(Object key) {
        return key instanceof CharSequence ? headers.get((CharSequence) key) : Collections.emptyList();
    }

    @Override
    public List<String> put(String key, List<String> value) {
        List<String> all = headers.get(key);
        headers.set(key, value);
        return all;
    }

    @Override
    public List<String> remove(Object key) {
        return key instanceof CharSequence ? headers.remove((CharSequence) key) : Collections.emptyList();
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        headers.set(m);
    }

    @Override
    public void clear() {
        headers.clear();
    }

    @Override
    public Set<String> keySet() {
        return headers.names();
    }

    @Override
    public Collection<List<String>> values() {
        Set<CharSequence> names = headers.nameSet();
        return new AbstractCollection<List<String>>() {
            @Override
            public Iterator<List<String>> iterator() {
                Iterator<CharSequence> it = names.iterator();
                return new Iterator<List<String>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public List<String> next() {
                        CharSequence next = it.next();
                        return next == null ? Collections.emptyList() : headers.get(next);
                    }
                };
            }

            @Override
            public int size() {
                return names.size();
            }
        };
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        Set<CharSequence> names = headers.nameSet();
        return new AbstractSet<Entry<String, List<String>>>() {
            @Override
            public Iterator<Entry<String, List<String>>> iterator() {
                Iterator<CharSequence> it = names.iterator();
                return new Iterator<Entry<String, List<String>>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Entry<String, List<String>> next() {
                        CharSequence next = it.next();
                        return new Entry<String, List<String>>() {
                            @Override
                            public String getKey() {
                                return next == null ? null : next.toString();
                            }

                            @Override
                            public List<String> getValue() {
                                return next == null ? Collections.emptyList() : get(next);
                            }

                            @Override
                            public List<String> setValue(List<String> value) {
                                if (next == null) {
                                    return Collections.emptyList();
                                }
                                List<String> values = get(next);
                                headers.set(next, value);
                                return values;
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return names.size();
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HttpHeadersMapAdapter && headers.equals(((HttpHeadersMapAdapter) obj).headers);
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }

    @Override
    public String toString() {
        return "HttpHeadersMapAdapter{" + "headers=" + headers + '}';
    }
}
