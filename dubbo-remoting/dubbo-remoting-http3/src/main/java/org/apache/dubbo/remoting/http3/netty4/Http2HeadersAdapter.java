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
package org.apache.dubbo.remoting.http3.netty4;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import io.netty.handler.codec.Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.incubator.codec.http3.Http3Headers;

public final class Http2HeadersAdapter implements Http2Headers {

    private final Http3Headers headers;

    public Http2HeadersAdapter(Http3Headers headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<Entry<CharSequence, CharSequence>> iterator() {
        return headers.iterator();
    }

    @Override
    public Iterator<CharSequence> valueIterator(CharSequence name) {
        return headers.valueIterator(name);
    }

    @Override
    public Http2Headers method(CharSequence value) {
        headers.method(value);
        return this;
    }

    @Override
    public Http2Headers scheme(CharSequence value) {
        headers.scheme(value);
        return this;
    }

    @Override
    public Http2Headers authority(CharSequence value) {
        headers.authority(value);
        return this;
    }

    @Override
    public Http2Headers path(CharSequence value) {
        headers.path(value);
        return this;
    }

    @Override
    public Http2Headers status(CharSequence value) {
        headers.status(value);
        return this;
    }

    @Override
    public CharSequence method() {
        return headers.method();
    }

    @Override
    public CharSequence scheme() {
        return headers.scheme();
    }

    @Override
    public CharSequence authority() {
        return headers.authority();
    }

    @Override
    public CharSequence path() {
        return headers.path();
    }

    @Override
    public CharSequence status() {
        return headers.status();
    }

    @Override
    public boolean contains(CharSequence name, CharSequence value, boolean caseInsensitive) {
        return headers.contains(name, value, caseInsensitive);
    }

    @Override
    public CharSequence get(CharSequence charSequence) {
        return headers.get(charSequence);
    }

    @Override
    public CharSequence get(CharSequence charSequence, CharSequence charSequence2) {
        return headers.get(charSequence, charSequence2);
    }

    @Override
    public CharSequence getAndRemove(CharSequence charSequence) {
        return headers.getAndRemove(charSequence);
    }

    @Override
    public CharSequence getAndRemove(CharSequence charSequence, CharSequence charSequence2) {
        return headers.getAndRemove(charSequence, charSequence2);
    }

    @Override
    public List<CharSequence> getAll(CharSequence charSequence) {
        return headers.getAll(charSequence);
    }

    @Override
    public List<CharSequence> getAllAndRemove(CharSequence charSequence) {
        return headers.getAllAndRemove(charSequence);
    }

    @Override
    public Boolean getBoolean(CharSequence charSequence) {
        return headers.getBoolean(charSequence);
    }

    @Override
    public boolean getBoolean(CharSequence charSequence, boolean b) {
        return headers.getBoolean(charSequence, b);
    }

    @Override
    public Byte getByte(CharSequence charSequence) {
        return headers.getByte(charSequence);
    }

    @Override
    public byte getByte(CharSequence charSequence, byte b) {
        return headers.getByte(charSequence, b);
    }

    @Override
    public Character getChar(CharSequence charSequence) {
        return headers.getChar(charSequence);
    }

    @Override
    public char getChar(CharSequence charSequence, char c) {
        return headers.getChar(charSequence, c);
    }

    @Override
    public Short getShort(CharSequence charSequence) {
        return headers.getShort(charSequence);
    }

    @Override
    public short getShort(CharSequence charSequence, short i) {
        return headers.getShort(charSequence, i);
    }

    @Override
    public Integer getInt(CharSequence charSequence) {
        return headers.getInt(charSequence);
    }

    @Override
    public int getInt(CharSequence charSequence, int i) {
        return headers.getInt(charSequence, i);
    }

    @Override
    public Long getLong(CharSequence charSequence) {
        return headers.getLong(charSequence);
    }

    @Override
    public long getLong(CharSequence charSequence, long l) {
        return headers.getLong(charSequence, l);
    }

    @Override
    public Float getFloat(CharSequence charSequence) {
        return headers.getFloat(charSequence);
    }

    @Override
    public float getFloat(CharSequence charSequence, float v) {
        return headers.getFloat(charSequence, v);
    }

    @Override
    public Double getDouble(CharSequence charSequence) {
        return headers.getDouble(charSequence);
    }

    @Override
    public double getDouble(CharSequence charSequence, double v) {
        return headers.getDouble(charSequence, v);
    }

    @Override
    public Long getTimeMillis(CharSequence charSequence) {
        return headers.getTimeMillis(charSequence);
    }

    @Override
    public long getTimeMillis(CharSequence charSequence, long l) {
        return headers.getTimeMillis(charSequence, l);
    }

    @Override
    public Boolean getBooleanAndRemove(CharSequence charSequence) {
        return headers.getBooleanAndRemove(charSequence);
    }

    @Override
    public boolean getBooleanAndRemove(CharSequence charSequence, boolean b) {
        return headers.getBooleanAndRemove(charSequence, b);
    }

    @Override
    public Byte getByteAndRemove(CharSequence charSequence) {
        return headers.getByteAndRemove(charSequence);
    }

    @Override
    public byte getByteAndRemove(CharSequence charSequence, byte b) {
        return headers.getByteAndRemove(charSequence, b);
    }

    @Override
    public Character getCharAndRemove(CharSequence charSequence) {
        return headers.getCharAndRemove(charSequence);
    }

    @Override
    public char getCharAndRemove(CharSequence charSequence, char c) {
        return headers.getCharAndRemove(charSequence, c);
    }

    @Override
    public Short getShortAndRemove(CharSequence charSequence) {
        return headers.getShortAndRemove(charSequence);
    }

    @Override
    public short getShortAndRemove(CharSequence charSequence, short i) {
        return headers.getShortAndRemove(charSequence, i);
    }

    @Override
    public Integer getIntAndRemove(CharSequence charSequence) {
        return headers.getIntAndRemove(charSequence);
    }

    @Override
    public int getIntAndRemove(CharSequence charSequence, int i) {
        return headers.getIntAndRemove(charSequence, i);
    }

    @Override
    public Long getLongAndRemove(CharSequence charSequence) {
        return headers.getLongAndRemove(charSequence);
    }

    @Override
    public long getLongAndRemove(CharSequence charSequence, long l) {
        return headers.getLongAndRemove(charSequence, l);
    }

    @Override
    public Float getFloatAndRemove(CharSequence charSequence) {
        return headers.getFloatAndRemove(charSequence);
    }

    @Override
    public float getFloatAndRemove(CharSequence charSequence, float v) {
        return headers.getFloatAndRemove(charSequence, v);
    }

    @Override
    public Double getDoubleAndRemove(CharSequence charSequence) {
        return headers.getDoubleAndRemove(charSequence);
    }

    @Override
    public double getDoubleAndRemove(CharSequence charSequence, double v) {
        return headers.getDoubleAndRemove(charSequence, v);
    }

    @Override
    public Long getTimeMillisAndRemove(CharSequence charSequence) {
        return headers.getTimeMillisAndRemove(charSequence);
    }

    @Override
    public long getTimeMillisAndRemove(CharSequence charSequence, long l) {
        return headers.getTimeMillisAndRemove(charSequence, l);
    }

    @Override
    public boolean contains(CharSequence charSequence) {
        return headers.contains(charSequence);
    }

    @Override
    public boolean contains(CharSequence charSequence, CharSequence charSequence2) {
        return headers.contains(charSequence, charSequence2);
    }

    @Override
    public boolean containsObject(CharSequence charSequence, Object o) {
        return headers.containsObject(charSequence, o);
    }

    @Override
    public boolean containsBoolean(CharSequence charSequence, boolean b) {
        return headers.containsBoolean(charSequence, b);
    }

    @Override
    public boolean containsByte(CharSequence charSequence, byte b) {
        return headers.containsByte(charSequence, b);
    }

    @Override
    public boolean containsChar(CharSequence charSequence, char c) {
        return headers.containsChar(charSequence, c);
    }

    @Override
    public boolean containsShort(CharSequence charSequence, short i) {
        return headers.containsShort(charSequence, i);
    }

    @Override
    public boolean containsInt(CharSequence charSequence, int i) {
        return headers.containsInt(charSequence, i);
    }

    @Override
    public boolean containsLong(CharSequence charSequence, long l) {
        return headers.containsLong(charSequence, l);
    }

    @Override
    public boolean containsFloat(CharSequence charSequence, float v) {
        return headers.containsFloat(charSequence, v);
    }

    @Override
    public boolean containsDouble(CharSequence charSequence, double v) {
        return headers.containsDouble(charSequence, v);
    }

    @Override
    public boolean containsTimeMillis(CharSequence charSequence, long l) {
        return headers.containsTimeMillis(charSequence, l);
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
    public Set<CharSequence> names() {
        return headers.names();
    }

    @Override
    public Http2Headers add(CharSequence charSequence, CharSequence charSequence2) {
        headers.add(charSequence, charSequence2);
        return this;
    }

    @Override
    public Http2Headers add(CharSequence charSequence, Iterable<? extends CharSequence> iterable) {
        headers.add(charSequence, iterable);
        return this;
    }

    @Override
    public Http2Headers add(CharSequence charSequence, CharSequence... charSequences) {
        headers.add(charSequence, charSequences);
        return this;
    }

    @Override
    public Http2Headers addObject(CharSequence charSequence, Object o) {
        headers.addObject(charSequence, o);
        return this;
    }

    @Override
    public Http2Headers addObject(CharSequence charSequence, Iterable<?> iterable) {
        headers.addObject(charSequence, iterable);
        return this;
    }

    @Override
    public Http2Headers addObject(CharSequence charSequence, Object... objects) {
        headers.addObject(charSequence, objects);
        return this;
    }

    @Override
    public Http2Headers addBoolean(CharSequence charSequence, boolean b) {
        headers.addBoolean(charSequence, b);
        return this;
    }

    @Override
    public Http2Headers addByte(CharSequence charSequence, byte b) {
        headers.addByte(charSequence, b);
        return this;
    }

    @Override
    public Http2Headers addChar(CharSequence charSequence, char c) {
        headers.addChar(charSequence, c);
        return this;
    }

    @Override
    public Http2Headers addShort(CharSequence charSequence, short i) {
        headers.addShort(charSequence, i);
        return this;
    }

    @Override
    public Http2Headers addInt(CharSequence charSequence, int i) {
        headers.addInt(charSequence, i);
        return this;
    }

    @Override
    public Http2Headers addLong(CharSequence charSequence, long l) {
        headers.addLong(charSequence, l);
        return this;
    }

    @Override
    public Http2Headers addFloat(CharSequence charSequence, float v) {
        headers.addFloat(charSequence, v);
        return this;
    }

    @Override
    public Http2Headers addDouble(CharSequence charSequence, double v) {
        headers.addDouble(charSequence, v);
        return this;
    }

    @Override
    public Http2Headers addTimeMillis(CharSequence charSequence, long l) {
        headers.addTimeMillis(charSequence, l);
        return this;
    }

    @Override
    public Http2Headers add(Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
        this.headers.add(headers);
        return this;
    }

    @Override
    public Http2Headers set(CharSequence charSequence, CharSequence charSequence2) {
        headers.set(charSequence, charSequence2);
        return this;
    }

    @Override
    public Http2Headers set(CharSequence charSequence, Iterable<? extends CharSequence> iterable) {
        headers.set(charSequence, iterable);
        return this;
    }

    @Override
    public Http2Headers set(CharSequence charSequence, CharSequence... charSequences) {
        headers.set(charSequence, charSequences);
        return this;
    }

    @Override
    public Http2Headers setObject(CharSequence charSequence, Object o) {
        headers.setObject(charSequence, o);
        return this;
    }

    @Override
    public Http2Headers setObject(CharSequence charSequence, Iterable<?> iterable) {
        headers.setObject(charSequence, iterable);
        return this;
    }

    @Override
    public Http2Headers setObject(CharSequence charSequence, Object... objects) {
        headers.setObject(charSequence, objects);
        return this;
    }

    @Override
    public Http2Headers setBoolean(CharSequence charSequence, boolean b) {
        headers.setBoolean(charSequence, b);
        return this;
    }

    @Override
    public Http2Headers setByte(CharSequence charSequence, byte b) {
        headers.setByte(charSequence, b);
        return this;
    }

    @Override
    public Http2Headers setChar(CharSequence charSequence, char c) {
        headers.setChar(charSequence, c);
        return this;
    }

    @Override
    public Http2Headers setShort(CharSequence charSequence, short i) {
        headers.setShort(charSequence, i);
        return this;
    }

    @Override
    public Http2Headers setInt(CharSequence charSequence, int i) {
        headers.setInt(charSequence, i);
        return this;
    }

    @Override
    public Http2Headers setLong(CharSequence charSequence, long l) {
        headers.setLong(charSequence, l);
        return this;
    }

    @Override
    public Http2Headers setFloat(CharSequence charSequence, float v) {
        headers.setFloat(charSequence, v);
        return this;
    }

    @Override
    public Http2Headers setDouble(CharSequence charSequence, double v) {
        headers.setDouble(charSequence, v);
        return this;
    }

    @Override
    public Http2Headers setTimeMillis(CharSequence charSequence, long l) {
        headers.setTimeMillis(charSequence, l);
        return this;
    }

    @Override
    public Http2Headers set(Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
        this.headers.set(headers);
        return this;
    }

    @Override
    public Http2Headers setAll(Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
        this.headers.setAll(headers);
        return this;
    }

    @Override
    public boolean remove(CharSequence charSequence) {
        return headers.remove(charSequence);
    }

    @Override
    public Http2Headers clear() {
        headers.clear();
        return this;
    }

    @Override
    public void forEach(Consumer<? super Entry<CharSequence, CharSequence>> action) {
        headers.forEach(action);
    }

    @Override
    public Spliterator<Entry<CharSequence, CharSequence>> spliterator() {
        return headers.spliterator();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(headers);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Http2Headers && headers.equals(obj);
    }

    @Override
    public String toString() {
        return headers.toString();
    }
}
