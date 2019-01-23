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
package org.apache.dubbo.common.serialize.fastjson;

import org.apache.dubbo.common.serialize.model.media.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FastJsonObjectOutputTest {
    private FastJsonObjectOutput fastJsonObjectOutput;
    private FastJsonObjectInput fastJsonObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.fastJsonObjectOutput = new FastJsonObjectOutput(byteArrayOutputStream);
    }

    @Test
    public void testWriteBool() throws IOException {
        this.fastJsonObjectOutput.writeBool(true);
        this.flushToInput();

        assertThat(fastJsonObjectInput.readBool(), is(true));
    }

    @Test
    public void testWriteShort() throws IOException {
        this.fastJsonObjectOutput.writeShort((short) 2);
        this.flushToInput();

        assertThat(fastJsonObjectInput.readShort(), is((short) 2));
    }

    @Test
    public void testWriteInt() throws IOException {
        this.fastJsonObjectOutput.writeInt(1);
        this.flushToInput();

        assertThat(fastJsonObjectInput.readInt(), is(1));
    }

    @Test
    public void testWriteLong() throws IOException {
        this.fastJsonObjectOutput.writeLong(1000L);
        this.flushToInput();

        assertThat(fastJsonObjectInput.readLong(), is(1000L));
    }

    @Test
    public void testWriteUTF() throws IOException {
        this.fastJsonObjectOutput.writeUTF("Pace Hasîtî 和平 Мир");
        this.flushToInput();

        assertThat(fastJsonObjectInput.readUTF(), is("Pace Hasîtî 和平 Мир"));
    }


    @Test
    public void testWriteFloat() throws IOException {
        this.fastJsonObjectOutput.writeFloat(1.88f);
        this.flushToInput();

        assertThat(this.fastJsonObjectInput.readFloat(), is(1.88f));
    }

    @Test
    public void testWriteDouble() throws IOException {
        this.fastJsonObjectOutput.writeDouble(1.66d);
        this.flushToInput();

        assertThat(this.fastJsonObjectInput.readDouble(), is(1.66d));
    }

    @Test
    public void testWriteBytes() throws IOException {
        this.fastJsonObjectOutput.writeBytes("hello".getBytes());
        this.flushToInput();

        assertThat(this.fastJsonObjectInput.readBytes(), is("hello".getBytes()));
    }

    @Test
    public void testWriteBytesWithSubLength() throws IOException {
        this.fastJsonObjectOutput.writeBytes("hello".getBytes(), 2, 2);
        this.flushToInput();

        assertThat(this.fastJsonObjectInput.readBytes(), is("ll".getBytes()));
    }

    @Test
    public void testWriteByte() throws IOException {
        this.fastJsonObjectOutput.writeByte((byte) 123);
        this.flushToInput();

        assertThat(this.fastJsonObjectInput.readByte(), is((byte) 123));
    }

    @Test
    public void testWriteObject() throws IOException, ClassNotFoundException {
        Image image = new Image("http://dubbo.io/logo.png", "logo", 300, 480, Image.Size.SMALL);
        this.fastJsonObjectOutput.writeObject(image);
        this.flushToInput();

        Image readObjectForImage = fastJsonObjectInput.readObject(Image.class);
        assertThat(readObjectForImage, not(nullValue()));
        assertThat(readObjectForImage, is(image));
    }

    private void flushToInput() throws IOException {
        this.fastJsonObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.fastJsonObjectInput = new FastJsonObjectInput(byteArrayInputStream);
    }
}