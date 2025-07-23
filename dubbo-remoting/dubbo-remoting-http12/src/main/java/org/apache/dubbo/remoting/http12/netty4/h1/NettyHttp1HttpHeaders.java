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
package org.apache.dubbo.remoting.http12.netty4.h1;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.message.HttpHeadersMapAdapter;
import org.apache.dubbo.remoting.http12.netty4.StringValueIterator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.netty.handler.codec.http.DefaultHttpHeaders;

public final class NettyHttp1HttpHeaders implements HttpHeaders {

    private final io.netty.handler.codec.http.HttpHeaders headers;

    public NettyHttp1HttpHeaders(io.netty.handler.codec.http.HttpHeaders headers) {
        this.headers = headers;
    }

    @SuppressWarnings("deprecation")
    public NettyHttp1HttpHeaders() {
        this(new DefaultHttpHeaders(false));
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
    public boolean containsKey(CharSequence key) {
        return headers.contains(key);
    }

    @Override
    public String getFirst(CharSequence name) {
        return headers.get(name);
    }

    @Override
    public List<String> get(CharSequence key) {
        return headers.getAll(key);
    }

    @Override
    public HttpHeaders add(CharSequence name, String value) {
        headers.add(name, value);
        return this;
    }

    public HttpHeaders add(CharSequence name, Iterable<String> values) {
        headers.add(name, values);
        return this;
    }

    @Override
    public HttpHeaders add(CharSequence name, String... values) {
        headers.add(name, Arrays.asList(values));
        return this;
    }

    @Override
    public HttpHeaders add(Map<? extends CharSequence, ? extends Iterable<String>> map) {
        for (Entry<? extends CharSequence, ? extends Iterable<String>> entry : map.entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public HttpHeaders add(HttpHeaders headers) {
        for (Entry<CharSequence, String> entry : headers) {
            this.headers.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public HttpHeaders set(CharSequence name, String value) {
        headers.set(name, value);
        return this;
    }

    public HttpHeaders set(CharSequence name, Iterable<String> values) {
        headers.set(name, values);
        return this;
    }

    @Override
    public HttpHeaders set(CharSequence name, String... values) {
        headers.set(name, Arrays.asList(values));
        return this;
    }

    @Override
    public HttpHeaders set(Map<? extends CharSequence, ? extends Iterable<String>> map) {
        for (Entry<? extends CharSequence, ? extends Iterable<String>> entry : map.entrySet()) {
            headers.set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public HttpHeaders set(HttpHeaders headers) {
        for (Entry<CharSequence, String> entry : headers) {
            this.headers.set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public List<String> remove(CharSequence key) {
        List<String> all = headers.getAll(key);
        headers.remove(key);
        return all;
    }

    @Override
    public void clear() {
        headers.clear();
    }

    @Override
    public Set<String> names() {
        return headers.names();
    }

    @Override
    public Set<CharSequence> nameSet() {
        if (isEmpty()) {
            return Collections.emptySet();
        }
        Set<CharSequence> names = new LinkedHashSet<>(headers.size());
        for (Entry<CharSequence, String> entry : this) {
            names.add(entry.getKey());
        }
        return names;
    }

    @Override
    public Map<String, List<String>> asMap() {
        return headers.isEmpty() ? Collections.emptyMap() : new HttpHeadersMapAdapter(this);
    }

    @Override
    public Iterator<Entry<CharSequence, String>> iterator() {
        return new StringValueIterator(headers.iteratorCharSequence());
    }

    public DefaultHttpHeaders getHeaders() {
        return (DefaultHttpHeaders) headers;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NettyHttp1HttpHeaders && headers.equals(((NettyHttp1HttpHeaders) obj).headers);
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }

    @Override
    public String toString() {
        return "NettyHttp1HttpHeaders{" + "headers=" + headers + '}';
    }
}
