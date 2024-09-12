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

import java.nio.charset.StandardCharsets;

import io.netty.util.internal.MathUtil;
import io.netty.util.internal.PlatformDependent;

/**
 * Zero-copy string as map key.
 */
public final class KeyString implements CharSequence {

    public static final KeyString EMPTY = new KeyString("");

    private final byte[] value;
    private final int offset;
    private final int length;
    private final boolean caseSensitive;

    private int hash;

    public KeyString(CharSequence value) {
        this(value, false);
    }

    public KeyString(CharSequence value, boolean caseSensitive) {
        this.value = toBytes(value);
        offset = 0;
        length = this.value.length;
        this.caseSensitive = caseSensitive;
    }

    private KeyString(byte[] value, int start, int end, boolean caseSensitive) {
        this.value = value;
        offset = start;
        length = end - start;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index: " + index + " must be in the range [0," + length + ")");
        }
        return (char) (PlatformDependent.getByte(value, index + offset) & 0xFF);
    }

    public byte[] array() {
        return value;
    }

    public int offset() {
        return offset;
    }

    public KeyString subSequence(int start) {
        return subSequence(start, length);
    }

    public KeyString subSequence(int start, int end) {
        int len = length;
        if (end < 0) {
            end = len;
        }
        if (MathUtil.isOutOfBounds(start, end - start, len)) {
            throw new IndexOutOfBoundsException(
                    "expected: 0 <= start(" + start + ") <= end (" + end + ") <= len(" + len + ')');
        }
        if (start == 0 && end == len) {
            return this;
        }
        if (end == start) {
            return EMPTY;
        }
        return new KeyString(value, start + offset, end + offset, caseSensitive);
    }

    public int indexOf(char ch, int start) {
        if (ch < 256) {
            if (start < 0) {
                start = 0;
            }
            byte b = (byte) ch;
            byte[] value = this.value;
            int offset = this.offset;
            for (int i = start + offset, len = length + offset; i < len; i++) {
                if (value[i] == b) {
                    return i - offset;
                }
            }
        }
        return -1;
    }

    public int lastIndexOf(char ch, int start) {
        if (ch < 256) {
            if (start < 0 || start >= length) {
                start = length - 1;
            }
            byte b = (byte) ch;
            byte[] value = this.value;
            int offset = this.offset;
            for (int i = offset + start; i >= offset; i--) {
                if (value[i] == b) {
                    return i - offset;
                }
            }
        }
        return -1;
    }

    public boolean regionMatches(int start, KeyString other) {
        return regionMatches(start, other, 0, other.length());
    }

    public boolean regionMatches(int start, KeyString other, int otherStart, int length) {
        if (other == null) {
            throw new NullPointerException("other");
        }
        if (otherStart < 0 || length > other.length() - otherStart) {
            return false;
        }
        if (start < 0 || length > length() - start) {
            return false;
        }
        if (caseSensitive) {
            return PlatformDependent.equals(value, offset, other.value, other.offset, length);
        }
        byte[] value = this.value, otherValue = other.value;
        byte a, b;
        for (int i = start + offset, j = otherStart + other.offset, end = i + length; i < end; i++, j++) {
            a = value[i];
            b = otherValue[j];
            if (a == b || toLowerCase(a) == toLowerCase(b)) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = PlatformDependent.hashCodeAscii(value, offset, length);
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != KeyString.class) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        KeyString other = (KeyString) obj;
        int len = length;
        if (len != other.length) {
            return false;
        }
        if (caseSensitive) {
            return PlatformDependent.equals(value, offset, other.value, other.offset, len);
        }
        byte[] value = this.value, otherValue = other.value;
        byte a, b;
        for (int i = offset, j = other.offset, end = i + len; i < end; i++, j++) {
            a = value[i];
            b = otherValue[j];
            if (a == b || toLowerCase(a) == toLowerCase(b)) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return toString(0, length);
    }

    public String toString(int start) {
        return toString(start, length);
    }

    @SuppressWarnings("deprecation")
    public String toString(int start, int end) {
        int len = length;
        if (end == -1) {
            end = len;
        }
        int count = end - start;
        if (count == 0) {
            return "";
        }
        if (MathUtil.isOutOfBounds(start, count, len)) {
            throw new IndexOutOfBoundsException(
                    "expected: " + "0 <= start(" + start + ") <= srcIdx + count(" + count + ") <= srcLen(" + len + ')');
        }
        return new String(value, 0, start + offset, count);
    }

    private static byte[] toBytes(CharSequence value) {
        if (value.getClass() == String.class) {
            return ((String) value).getBytes(StandardCharsets.ISO_8859_1);
        }
        int len = value.length();
        byte[] array = PlatformDependent.allocateUninitializedArray(len);
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            array[i] = (byte) (c > 255 ? '?' : c);
        }
        return array;
    }

    private static byte toLowerCase(byte value) {
        return value >= 'A' && value <= 'Z' ? (byte) (value + 32) : value;
    }
}
