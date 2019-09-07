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
package org.apache.dubbo.common.serialize.protobuf.support;

import org.apache.dubbo.common.serialize.protobuf.support.model.GooglePB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class GenericProtobufObjectOutputTest {
    private ByteArrayOutputStream byteArrayOutputStream;
    private GenericProtobufObjectOutput genericProtobufObjectOutput;
    private GenericProtobufObjectInput genericProtobufObjectInput;
    private ByteArrayInputStream byteArrayInputStream;

    @BeforeEach
    public void setUp() {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.genericProtobufObjectOutput = new GenericProtobufObjectOutput(byteArrayOutputStream);
    }

    @Test
    public void testWriteObjectNull() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            this.genericProtobufObjectOutput.writeObject(null);
        });
    }

    @Test
    public void testWriteGooglePbObject() throws IOException {
        Random random = new Random();
        final int bound = 100000;
        List<GooglePB.PhoneNumber> phoneNumberList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            phoneNumberList.add(GooglePB.PhoneNumber.newBuilder().setNumber(random.nextInt(bound) + "").setType(GooglePB.PhoneType.forNumber(random.nextInt(GooglePB.PhoneType.values().length - 1))).build());
        }

        Map<String, GooglePB.PhoneNumber> phoneNumberMap = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            phoneNumberMap.put("phoneNumber" + i, GooglePB.PhoneNumber.newBuilder().setNumber(random.nextInt(bound) + "").setType(GooglePB.PhoneType.forNumber(random.nextInt(GooglePB.PhoneType.values().length - 1))).build());
        }
        GooglePB.PBRequestType request = GooglePB.PBRequestType.newBuilder()
                .setAge(15).setCash(10).setMoney(16.0).setNum(100L)
                .addAllPhone(phoneNumberList).putAllDoubleMap(phoneNumberMap).build();

        this.genericProtobufObjectOutput.writeObject(request);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readObject(GooglePB.PBRequestType.class), is(request));
    }

    @Test
    public void testWriteBoolean() throws IOException {
        boolean random = new Random().nextBoolean();
        this.genericProtobufObjectOutput.writeBool(random);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readBool(), is(random));
    }

    @Test
    public void testWriteByte() throws IOException {
        int random = new Random().nextInt();
        this.genericProtobufObjectOutput.writeByte((byte) random);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readByte(), is((byte) random));
    }

    @Test
    public void testWriteShort() throws IOException {
        int random = new Random().nextInt();
        this.genericProtobufObjectOutput.writeShort((short) random);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readShort(), is((short) random));
    }

    @Test
    public void testWriteInt() throws IOException {
        int random = new Random().nextInt();
        this.genericProtobufObjectOutput.writeInt(random);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readInt(), is(random));
    }

    @Test
    public void testWriteFloat() throws IOException {
        float random = new Random().nextFloat();
        this.genericProtobufObjectOutput.writeFloat(random);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readFloat(), is(random));
    }


    @Test
    public void testWriteDouble() throws IOException {
        double random = new Random().nextDouble();
        this.genericProtobufObjectOutput.writeDouble(random);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readDouble(), is(random));
    }


    @Test
    public void testWriteString() throws IOException {
        byte[] bytes = new byte[new Random().nextInt(100)];
        new Random().nextBytes(bytes);

        this.genericProtobufObjectOutput.writeUTF(new String(bytes));
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readUTF(), is(new String(bytes)));
    }

    @Test
    public void testWriteBytes() throws IOException {
        byte[] bytes = new byte[new Random().nextInt(100)];
        new Random().nextBytes(bytes);
        this.genericProtobufObjectOutput.writeBytes(bytes);
        this.flushToInput();
        final byte[] bytes1 = genericProtobufObjectInput.readBytes();
        assertThat(bytes1, is(bytes));
    }

    @Test
    public void testWriteBytesSpecLength() throws IOException {
        final int length = new Random().nextInt(100);
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        this.genericProtobufObjectOutput.writeBytes(bytes, 0, length);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readBytes(), is(bytes));
    }

    @Test
    public void testWriteLong() throws IOException {
        long random = new Random().nextLong();
        this.genericProtobufObjectOutput.writeLong(random);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readLong(), is(random));
    }


    @Test
    public void testWriteMap() throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("key", "hello");
        map.put("value", "dubbo");
        this.genericProtobufObjectOutput.writeObject(map);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readObject(Map.class), is(map));
    }


    @Test
    void testWriteMultiType() throws IOException {
        long random = new Random().nextLong();
        this.genericProtobufObjectOutput.writeLong(random);
        Map<String, String> map = new HashMap<>();
        map.put("key", "hello");
        map.put("value", "world");
        this.genericProtobufObjectOutput.writeObject(map);
        final int length = new Random().nextInt(100);
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        this.genericProtobufObjectOutput.writeBytes(bytes, 0, length);
        int randomShort = new Random().nextInt();
        this.genericProtobufObjectOutput.writeShort((short) randomShort);
        this.flushToInput();
        assertThat(genericProtobufObjectInput.readLong(), is(random));
        assertThat(genericProtobufObjectInput.readObject(Map.class), is(map));
        assertThat(genericProtobufObjectInput.readBytes(), is(bytes));
        assertThat(genericProtobufObjectInput.readShort(), is((short) randomShort));
    }

    private void flushToInput() {
        this.genericProtobufObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.genericProtobufObjectInput = new GenericProtobufObjectInput(byteArrayInputStream);
    }
}

