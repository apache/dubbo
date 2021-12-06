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
package org.apache.dubbo.common.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UnsafeStringWriterTest {
    @Test
    public void testWrite() {
        UnsafeStringWriter writer = new UnsafeStringWriter();
        writer.write("a");
        writer.write("abc", 1, 1);
        writer.write(99);
        writer.flush();
        writer.close();

        assertThat(writer.toString(), is("abc"));
    }

    @Test
    public void testNegativeSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UnsafeStringWriter(-1));
    }

    @Test
    public void testAppend() {
        UnsafeStringWriter writer = new UnsafeStringWriter();
        writer.append("a");
        writer.append("abc", 1, 2);
        writer.append('c');
        writer.flush();
        writer.close();

        assertThat(writer.toString(), is("abc"));
    }

    @Test
    public void testAppendNull() {
        UnsafeStringWriter writer = new UnsafeStringWriter();
        writer.append(null);
        writer.append(null, 0, 4);
        writer.flush();
        writer.close();

        assertThat(writer.toString(), is("nullnull"));
    }

    @Test
    public void testWriteNull() throws IOException {
        UnsafeStringWriter writer = new UnsafeStringWriter(3);
        char[] chars = new char[2];
        chars[0] = 'a';
        chars[1] = 'b';
        writer.write(chars);
        writer.write(chars, 0, 1);
        writer.flush();
        writer.close();

        assertThat(writer.toString(), is("aba"));
    }

    @Test
    public void testWriteCharWithWrongLength() throws IOException {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            UnsafeStringWriter writer = new UnsafeStringWriter();
            char[] chars = new char[0];
            writer.write(chars, 0, 1);
        });
    }

    @Test
    public void testWriteCharWithWrongCombineLength() throws IOException {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            UnsafeStringWriter writer = new UnsafeStringWriter();
            char[] chars = new char[1];
            writer.write(chars, 1, 1);
        });
    }
}
