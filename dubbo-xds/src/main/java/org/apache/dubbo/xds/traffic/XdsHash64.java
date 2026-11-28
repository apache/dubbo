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
package org.apache.dubbo.xds.traffic;

/** XXH64, seed zero, for gRFC A42 request and ring keys. */
final class XdsHash64 {
    private static final long P1 = 0x9e3779b185ebca87L;
    private static final long P2 = 0xc2b2ae3d27d4eb4fL;
    private static final long P3 = 0x165667b19e3779f9L;
    private static final long P4 = 0x85ebca77c2b2ae63L;
    private static final long P5 = 0x27d4eb2f165667c5L;

    private XdsHash64() {}

    static long string(String value) {
        byte[] bytes = new byte[value.length()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) value.charAt(i);
        return bytes(bytes);
    }

    static long number(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) bytes[i] = (byte) (value >>> (8 * i));
        return bytes(bytes);
    }

    static long bytes(byte[] input) {
        int index = 0;
        long hash;
        if (input.length >= 32) {
            long a = P1 + P2;
            long b = P2;
            long c = 0;
            long d = -P1;
            do {
                a = round(a, little(input, index, 8));
                b = round(b, little(input, index + 8, 8));
                c = round(c, little(input, index + 16, 8));
                d = round(d, little(input, index + 24, 8));
                index += 32;
            } while (index <= input.length - 32);
            hash = Long.rotateLeft(a, 1) + Long.rotateLeft(b, 7) + Long.rotateLeft(c, 12) + Long.rotateLeft(d, 18);
            hash = merge(merge(merge(merge(hash, a), b), c), d);
        } else hash = P5;
        hash += input.length;
        while (index <= input.length - 8) {
            hash = Long.rotateLeft(hash ^ round(0, little(input, index, 8)), 27) * P1 + P4;
            index += 8;
        }
        if (index <= input.length - 4) {
            hash = Long.rotateLeft(hash ^ little(input, index, 4) * P1, 23) * P2 + P3;
            index += 4;
        }
        while (index < input.length) {
            hash = Long.rotateLeft(hash ^ (input[index++] & 0xffL) * P5, 11) * P1;
        }
        hash ^= hash >>> 33;
        hash *= P2;
        hash ^= hash >>> 29;
        hash *= P3;
        return hash ^ (hash >>> 32);
    }

    private static long round(long value, long block) {
        return Long.rotateLeft(value + block * P2, 31) * P1;
    }

    private static long merge(long hash, long value) {
        return (hash ^ round(0, value)) * P1 + P4;
    }

    private static long little(byte[] input, int start, int length) {
        long value = 0;
        for (int i = 0; i < length; i++) value |= (input[start + i] & 0xffL) << (8 * i);
        return value;
    }
}
