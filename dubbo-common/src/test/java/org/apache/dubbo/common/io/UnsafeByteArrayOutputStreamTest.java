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
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

public class UnsafeByteArrayOutputStreamTest {
    @Test
    public void testWrongSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UnsafeByteArrayOutputStream(-1));
    }

    @Test
    public void testWrite() {
        UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream(1);
        outputStream.write((int) 'a');
        outputStream.write("bc".getBytes(), 0, 2);

        assertThat(outputStream.size(), is(3));
        assertThat(outputStream.toString(), is("abc"));
    }

    @Test
    public void testToByteBuffer() {
        UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream(1);
        outputStream.write((int) 'a');

        ByteBuffer byteBuffer = outputStream.toByteBuffer();
        assertThat(byteBuffer.get(), is("a".getBytes()[0]));
    }

    @Test
    public void testExtendLengthForBuffer() throws IOException {
        UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream(1);
        for (int i = 0; i < 10; i++) {
            outputStream.write(i);
        }
        assertThat(outputStream.size(), is(10));

        OutputStream stream = mock(OutputStream.class);
        outputStream.writeTo(stream);
        Mockito.verify(stream).write(any(byte[].class), anyInt(), eq(10));
    }

    @Test
    public void testToStringWithCharset() throws IOException {
        UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream();
        outputStream.write("Hòa Bình".getBytes());

        assertThat(outputStream.toString("UTF-8"), is("Hòa Bình"));
    }
}