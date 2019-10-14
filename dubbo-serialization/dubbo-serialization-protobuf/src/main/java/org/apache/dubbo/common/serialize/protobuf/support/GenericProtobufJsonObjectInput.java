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

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.protobuf.support.wrapper.MapValue;
import org.apache.dubbo.common.serialize.protobuf.support.wrapper.ThrowablePB;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.HEARTBEAT_EVENT;
import static org.apache.dubbo.common.constants.CommonConstants.MOCK_HEARTBEAT_EVENT;

/**
 * GenericGoogleProtobuf object input implementation
 */
public class GenericProtobufJsonObjectInput implements ObjectInput {
    private final BufferedReader reader;

    public GenericProtobufJsonObjectInput(InputStream in) {
        this.reader = new BufferedReader(new InputStreamReader(in));
    }

    @Override
    public boolean readBool() throws IOException {
        return read(BoolValue.class).getValue();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) read(Int32Value.class).getValue();
    }

    @Override
    public short readShort() throws IOException {
        return (short) read(Int32Value.class).getValue();
    }

    @Override
    public int readInt() throws IOException {
        return read(Int32Value.class).getValue();
    }

    @Override
    public long readLong() throws IOException {
        return read(Int64Value.class).getValue();
    }

    @Override
    public float readFloat() throws IOException {
        return read(FloatValue.class).getValue();
    }

    @Override
    public double readDouble() throws IOException {
        return read(DoubleValue.class).getValue();
    }

    @Override
    public String readUTF() throws IOException {
        return read(StringValue.class).getValue();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return read(BytesValue.class).getValue().toByteArray();
    }

    @Override
    public Object readObject() {
        throw new UnsupportedOperationException("Provide the protobuf message type you want to read.");
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException {
        return read(cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException {
        return readObject(cls);
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        if (line == null || line.trim().length() == 0) {
            throw new EOFException();
        }
        return line;
    }

    private <T> T read(Class<T> cls) throws IOException {
        if (!ProtobufUtils.isSupported(cls)) {
            throw new IllegalArgumentException("This serialization only support google protobuf entity, the class is :" + cls.getName());
        }

        String json = readLine();
        return ProtobufUtils.deserializeJson(json, cls);
    }

    @Override
    public Throwable readThrowable() throws IOException {
        String json = readLine();
        ThrowablePB.ThrowableProto throwableProto = ProtobufUtils.deserializeJson(json, ThrowablePB.ThrowableProto.class);
        return ProtobufUtils.convertToException(throwableProto);
    }

    /**
     * FIXME, only supports transmission of String values.
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public Map<String, Object> readAttachments() throws IOException, ClassNotFoundException {
        String json = readLine();
        Map<String, String> attachments = ProtobufUtils.deserializeJson(json, MapValue.Map.class).getAttachmentsMap();
        Map<String, Object> genericAttachments = new HashMap<>();
        attachments.forEach((k, v) -> {
            genericAttachments.put(k, v);
        });
        return genericAttachments;
    }

    @Override
    public Object readEvent() throws IOException, ClassNotFoundException {
        String eventData = readUTF();
        if (eventData.equals(MOCK_HEARTBEAT_EVENT)) {
            eventData = HEARTBEAT_EVENT;
        }
        return eventData;
    }
}
