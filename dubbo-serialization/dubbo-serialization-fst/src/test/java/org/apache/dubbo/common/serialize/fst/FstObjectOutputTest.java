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
package org.apache.dubbo.common.serialize.fst;

import org.apache.dubbo.common.serialize.fst.model.AnimalEnum;
import org.apache.dubbo.common.serialize.fst.model.FullAddress;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class FstObjectOutputTest {
    private FstObjectOutput fstObjectOutput;
    private FstObjectInput fstObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;

    @Before
    public void setUp() {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.fstObjectOutput = new FstObjectOutput(byteArrayOutputStream);
    }


    @Test
    public void testWriteBool() throws IOException {
        this.fstObjectOutput.writeBool(false);
        this.flushToInput();

        boolean result = this.fstObjectInput.readBool();
        assertThat(result, is(false));
    }


    @Test
    public void testWriteUTF() throws IOException {
        this.fstObjectOutput.writeUTF("I don’t know 知りません Не знаю");
        this.flushToInput();

        String result = this.fstObjectInput.readUTF();
        assertThat(result, is("I don’t know 知りません Не знаю"));
    }

    @Test
    public void testWriteShort() throws IOException {
        this.fstObjectOutput.writeShort((short) 1);
        this.flushToInput();

        Short result = this.fstObjectInput.readShort();
        assertThat(result, is((short) 1));
    }

    @Test
    public void testWriteLong() throws IOException {
        this.fstObjectOutput.writeLong(12345678L);
        this.flushToInput();

        Long result = this.fstObjectInput.readLong();
        assertThat(result, is(12345678L));
    }

    @Test
    public void testWriteDouble() throws IOException {
        this.fstObjectOutput.writeDouble(-1.66d);
        this.flushToInput();

        Double result = this.fstObjectInput.readDouble();
        assertThat(result, is(-1.66d));
    }


    @Test
    public void testWriteInt() throws IOException {
        this.fstObjectOutput.writeInt(1);
        this.flushToInput();

        Integer result = this.fstObjectInput.readInt();
        assertThat(result, is(1));
    }

    @Test
    public void testWriteByte() throws IOException {
        this.fstObjectOutput.writeByte((byte) 222);
        this.flushToInput();

        Byte result = this.fstObjectInput.readByte();
        assertThat(result, is(((byte) 222)));
    }

    @Test
    public void testWriteBytesWithSubLength() throws IOException {
        this.fstObjectOutput.writeBytes("who are you".getBytes(), 4, 3);
        this.flushToInput();

        byte[] result = this.fstObjectInput.readBytes();
        assertThat(result, is("are".getBytes()));
    }

    @Test
    public void testWriteBytes() throws IOException {
        this.fstObjectOutput.writeBytes("who are you".getBytes());
        this.flushToInput();

        byte[] result = this.fstObjectInput.readBytes();
        assertThat(result, is("who are you".getBytes()));
    }

    @Test
    public void testWriteFloat() throws IOException {
        this.fstObjectOutput.writeFloat(-666.66f);
        this.flushToInput();

        Float result = this.fstObjectInput.readFloat();
        assertThat(result, is(-666.66f));
    }

    @Test
    public void testWriteNullBytesWithSubLength() throws IOException {
        this.fstObjectOutput.writeBytes(null, 4, 3);
        this.flushToInput();

        byte[] result = this.fstObjectInput.readBytes();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void testWriteNullBytes() throws IOException {
        this.fstObjectOutput.writeBytes(null);
        this.flushToInput();

        byte[] result = this.fstObjectInput.readBytes();
        assertThat(result, is(nullValue()));
    }


    @Test
    public void testWriteObject() throws IOException, ClassNotFoundException {
        FullAddress fullAddress = new FullAddress("cId", "pN", "cityId", "Nan Long Street", "51000");
        this.fstObjectOutput.writeObject(fullAddress);
        this.flushToInput();

        FullAddress result = this.fstObjectInput.readObject(FullAddress.class);
        assertThat(result, is(fullAddress));
    }

    @Test
    public void testWriteEnum() throws IOException, ClassNotFoundException {
        this.fstObjectOutput.writeObject(AnimalEnum.cat);
        this.flushToInput();

        AnimalEnum animalEnum = (AnimalEnum) this.fstObjectInput.readObject();
        assertThat(animalEnum, is(AnimalEnum.cat));
    }

    private void flushToInput() throws IOException {
        this.fstObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.fstObjectInput = new FstObjectInput(byteArrayInputStream);
    }
}