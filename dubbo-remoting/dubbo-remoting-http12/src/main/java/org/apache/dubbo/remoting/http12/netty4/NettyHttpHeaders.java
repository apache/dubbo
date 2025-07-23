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
package org.apache.dubbo.remoting.http12.netty4;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.message.HttpHeadersMapAdapter;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.netty.handler.codec.Headers;

@SuppressWarnings({"unchecked", "rawtypes"})
public class NettyHttpHeaders<T extends Headers<CharSequence, CharSequence, ?>> implements HttpHeaders {

    private final T headers;

    public NettyHttpHeaders(T headers) {
        this.headers = headers;
    }

    @Override
    public final int size() {
        return headers.size();
    }

    @Override
    public final boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public final boolean containsKey(CharSequence key) {
        return headers.contains(key);
    }

    @Override
    public final String getFirst(CharSequence name) {
        CharSequence value = headers.get(name);
        return value == null ? null : value.toString();
    }

    @Override
    public final List<String> get(CharSequence key) {
        List<CharSequence> all = headers.getAll(key);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        ListIterator<CharSequence> it = all.listIterator();
        while (it.hasNext()) {
            CharSequence next = it.next();
            if (next != null && next.getClass() != String.class) {
                it.set(next.toString());
            }
        }
        return (List) all;
    }

    @Override
    public final HttpHeaders add(CharSequence name, String value) {
        headers.add(name, value);
        return this;
    }

    public final HttpHeaders add(CharSequence name, Iterable<String> values) {
        headers.add(name, values);
        return this;
    }

    @Override
    public final HttpHeaders add(CharSequence name, String... values) {
        if (values != null && values.length != 0) {
            headers.add(name, Arrays.asList(values));
        }
        return this;
    }

    @Override
    public final HttpHeaders add(Map<? extends CharSequence, ? extends Iterable<String>> map) {
        for (Entry<? extends CharSequence, ? extends Iterable<String>> entry : map.entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public final HttpHeaders add(HttpHeaders headers) {
        for (Entry<CharSequence, String> entry : headers) {
            this.headers.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public final HttpHeaders set(CharSequence name, String value) {
        headers.set(name, value);
        return this;
    }

    public final HttpHeaders set(CharSequence name, Iterable<String> values) {
        headers.set(name, values);
        return this;
    }

    @Override
    public final HttpHeaders set(CharSequence name, String... values) {
        if (values != null && values.length != 0) {
            headers.set(name, Arrays.asList(values));
        }
        return this;
    }

    @Override
    public final HttpHeaders set(Map<? extends CharSequence, ? extends Iterable<String>> map) {
        for (Entry<? extends CharSequence, ? extends Iterable<String>> entry : map.entrySet()) {
            headers.set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public final HttpHeaders set(HttpHeaders headers) {
        for (Entry<CharSequence, String> entry : headers) {
            this.headers.set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public final List<String> remove(CharSequence key) {
        return (List) headers.getAllAndRemove(key);
    }

    @Override
    public final void clear() {
        headers.clear();
    }

    @Override
    public final Set<String> names() {
        Set<CharSequence> names = headers.names();
        return new AbstractSet<String>() {
            @Override
            public Iterator<String> iterator() {
                Iterator<CharSequence> it = names.iterator();
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public String next() {
                        CharSequence next = it.next();
                        return next == null ? null : next.toString();
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return names.size();
            }

            @Override
            public boolean contains(Object o) {
                return names.contains(o);
            }
        };
    }

    @Override
    public Set<CharSequence> nameSet() {
        return headers.names();
    }

    @Override
    public final Map<String, List<String>> asMap() {
        return headers.isEmpty() ? Collections.emptyMap() : new HttpHeadersMapAdapter(this);
    }

    @Override
    public final Iterator<Entry<CharSequence, String>> iterator() {
        return new StringValueIterator(headers.iterator());
    }

    public final T getHeaders() {
        return headers;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NettyHttpHeaders && headers.equals(((NettyHttpHeaders<?>) obj).headers);
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "headers=" + headers + '}';
    }
}
