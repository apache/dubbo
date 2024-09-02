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
package org.apache.dubbo.rpc.protocol.tri.rest.util;

/**
 * Zero-copy string as map key.
 */
public final class KeyString implements CharSequence {

    private final String value;
    private final int offset;
    private final int length;
    private final boolean caseSensitive;

    public KeyString(String value, int start, int end, boolean caseSensitive) {
        this.value = value;
        offset = start;
        length = (end == -1 ? value.length() : end) - start;
        this.caseSensitive = caseSensitive;
    }

    public KeyString(String value, int start, int end) {
        this(value, start, end, true);
    }

    public KeyString(String value, int end, boolean caseSensitive) {
        this.value = value;
        offset = 0;
        length = end == -1 ? value.length() : end;
        this.caseSensitive = caseSensitive;
    }

    public KeyString(String value, int end) {
        this(value, end, true);
    }

    public KeyString(String value, boolean caseSensitive) {
        this.value = value;
        offset = 0;
        length = value.length();
        this.caseSensitive = caseSensitive;
    }

    public KeyString(String value) {
        this(value, false);
    }

    public KeyString(KeyString path, int start, int end) {
        this(path.value, path.offset + start, path.offset + end, path.caseSensitive);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return value.charAt(offset + index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return value.substring(offset + start, offset + end);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < length; i++) {
            h = 31 * h + (caseSensitive ? value.charAt(offset + i) : Character.toLowerCase(value.charAt(offset + i)));
        }
        return h;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        KeyString other = (KeyString) obj;
        return value.regionMatches(!caseSensitive, offset, other.value, other.offset, length);
    }

    @Override
    public String toString() {
        return value.substring(offset, offset + length);
    }

    public int indexOf(char ch, int start) {
        int index = value.indexOf(ch, offset + start);
        return index == -1 || index >= offset + length ? -1 : index - offset;
    }

    public boolean regionMatches(int start, String value, int i, int length) {
        return this.value.regionMatches(!caseSensitive, offset + start, value, i, length);
    }

    public String substring(int start) {
        return value.substring(offset + start, offset + length);
    }

    public String substring(int start, int end) {
        return value.substring(offset + start, offset + (end == -1 ? length : end));
    }
}
