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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.serialize.protobuf.support.model.GooglePB;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractProtobufSerializationTest {
    protected static Random random = new Random();
    protected URL url = new URL("protocol", "1.1.1.1", 1234);
    protected ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    protected Serialization serialization = new GenericProtobufSerialization();

    @Test
    public void test_Bool() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeBool(false);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertFalse(deserialize.readBool());

        try {
            deserialize.readBool();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Bool_Multi() throws Exception {
        boolean[] array = new boolean[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextBoolean();
        }

        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        for (boolean b : array) {
            objectOutput.writeBool(b);
        }
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        for (boolean b : array) {
            assertEquals(b, deserialize.readBool());
        }

        try {
            deserialize.readBool();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Byte() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeByte((byte) 123);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertEquals((byte) 123, deserialize.readByte());

        try {
            deserialize.readByte();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Byte_Multi() throws Exception {
        byte[] array = new byte[100];
        random.nextBytes(array);

        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        for (byte b : array) {
            objectOutput.writeByte(b);
        }
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        for (byte b : array) {
            assertEquals(b, deserialize.readByte());
        }

        try {
            deserialize.readByte();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Short() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeShort((short) 123);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertEquals((short) 123, deserialize.readShort());

        try {
            deserialize.readShort();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Integer() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeInt(1);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        int i = deserialize.readInt();
        assertEquals(1, i);

        try {
            deserialize.readInt();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Long() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeLong(123L);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertEquals(123L, deserialize.readLong());

        try {
            deserialize.readLong();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Float() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeFloat(1.28F);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertEquals(1.28F, deserialize.readFloat());

        try {
            deserialize.readFloat();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    // ================== Util methods ==================

    @Test
    public void test_Double() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeDouble(1.28);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertEquals(1.28, deserialize.readDouble());

        try {
            deserialize.readDouble();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_UtfString() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeUTF("123中华人民共和国");
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertEquals("123中华人民共和国", deserialize.readUTF());

        try {
            deserialize.readUTF();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_Bytes() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeBytes("123中华人民共和国".getBytes());
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        assertArrayEquals("123中华人民共和国".getBytes(), deserialize.readBytes());

        try {
            deserialize.readBytes();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void test_BytesRange() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeBytes("123中华人民共和国-新疆维吾尔自治区".getBytes(), 1, 9);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        byte[] expectedArray = new byte[9];
        System.arraycopy("123中华人民共和国-新疆维吾尔自治区".getBytes(), 1, expectedArray, 0, expectedArray.length);
        assertArrayEquals(expectedArray, deserialize.readBytes());

        try {
            deserialize.readBytes();
            fail();
        } catch (Exception expected) {
            expected.printStackTrace();
        }
    }

    private GooglePB.PBRequestType buildPbMessage() {
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
        return request;
    }

    @Test
    public void testPbNormal() throws Exception {
        ProtobufUtils.marshaller(GooglePB.PBRequestType.getDefaultInstance());
        GooglePB.PBRequestType request = buildPbMessage();
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeObject(request);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput objectInput = serialization.deserialize(url, byteArrayInputStream);

        GooglePB.PBRequestType derializedRequest = objectInput.readObject(GooglePB.PBRequestType.class);
        assertEquals(request, derializedRequest);
    }

    /**
     * Special test case
     * Dubbo protocol will directly writes native map (Invocation.attachments) using protobuf.
     * this should definitely be fixed but not done yet.
     */
    @Test
    public void testPbMap() throws Exception {
        Map<String, Object> attachments = new HashMap<>();
        attachments.put("key", "value");
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeAttachments(attachments);
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput objectInput = serialization.deserialize(url, byteArrayInputStream);

        Map<String, Object> derializedAttachments = objectInput.readAttachments();
        assertEquals(attachments, derializedAttachments);
    }

    @Test
    public void testPbThrowable() {

    }

    @Test
    public void testNotPb() {

    }

}
