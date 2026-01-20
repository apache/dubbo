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
package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Utf8UtilsTest {

    @Test
    void shouldDecodeBasicAscii() {
        // Just checking if it handles normal English letters (1 byte each)
        byte[] bytes = "Dubbo".getBytes();
        char[] dest = new char[bytes.length];

        int resultLen = Utf8Utils.decodeUtf8(bytes, 0, bytes.length, dest, 0);

        assertEquals(5, resultLen);
        assertEquals("Dubbo", new String(dest));
    }

    @Test
    void shouldHandleMultiByteCharacters() {
        String input = "你好";
        byte[] bytes = input.getBytes();
        // Give it more space (6 instead of 2) to match the byte length
        char[] dest = new char[bytes.length];

        int resultLen = Utf8Utils.decodeUtf8(bytes, 0, bytes.length, dest, 0);

        assertEquals(2, resultLen);
        // We trim the string to only the parts that were actually filled
        assertEquals("你好", new String(dest, 0, resultLen));
    }

    @Test
    void shouldFailOnInvalidInput() {
        // Poking the machine with a broken byte it shouldn't recognize
        // 0xFE is not a valid UTF-8 start byte
        byte[] badBytes = {(byte) 0xFE};
        char[] dest = new char[1];

        assertThrows(IllegalArgumentException.class, () -> {
            Utf8Utils.decodeUtf8(badBytes, 0, 1, dest, 0);
        });
    }

    @Test
    void shouldHandleEmptyInput() {
        // Making sure it doesn't freak out if we give it nothing
        byte[] empty = new byte[0];
        char[] dest = new char[0];

        int result = Utf8Utils.decodeUtf8(empty, 0, 0, dest, 0);
        assertEquals(0, result);
    }
}
