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

import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.protobuf.support.wrapper.MapValue;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.MessageLite;
import com.google.protobuf.StringValue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.HEARTBEAT_EVENT;
import static org.apache.dubbo.common.constants.CommonConstants.MOCK_HEARTBEAT_EVENT;

/**
 * GenericGoogleProtobuf object output implementation
 */
public class GenericProtobufObjectOutput implements ObjectOutput {

    private final OutputStream os;

    public GenericProtobufObjectOutput(OutputStream os) {
        this.os = os;
    }

    @Override
    public void writeBool(boolean v) throws IOException {

        writeObject(BoolValue.newBuilder().setValue(v).build());
    }

    @Override
    public void writeByte(byte v) throws IOException {
        writeObject(Int32Value.newBuilder().setValue((v)).build());
    }

    @Override
    public void writeShort(short v) throws IOException {
        writeObject(Int32Value.newBuilder().setValue(v).build());
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeObject(Int32Value.newBuilder().setValue(v).build());
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeObject(Int64Value.newBuilder().setValue(v).build());
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeObject(FloatValue.newBuilder().setValue(v).build());
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeObject(DoubleValue.newBuilder().setValue(v).build());
    }

    @Override
    public void writeUTF(String v) throws IOException {
        writeObject(StringValue.newBuilder().setValue(v).build());
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        writeObject(BytesValue.newBuilder().setValue(ByteString.copyFrom(b)).build());
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        writeObject(BytesValue.newBuilder().setValue(ByteString.copyFrom(b, off, len)).build());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeObject(Object obj) throws IOException {
        /**
         * Protobuf does not allow writing of non-protobuf generated messages, including null value.
         * Writing of null value from developers should be denied immediately by throwing exception.
         */
        if (obj == null) {
            throw new IllegalStateException("This serialization only supports google protobuf objects, " +
                    "please use com.google.protobuf.Empty instead if you want to transmit null values.");
            // obj = ProtobufUtils.convertNullToEmpty();
        }
        if (!ProtobufUtils.isSupported(obj.getClass())) {
            throw new IllegalArgumentException("This serialization only supports google protobuf objects, current object class is: " + obj.getClass().getName());
        }

        ProtobufUtils.serialize(obj, os);
        os.flush();
    }

    @Override
    public void writeEvent(Object data) throws IOException {
        if (data == HEARTBEAT_EVENT) {
            data = MOCK_HEARTBEAT_EVENT;
        }
        writeUTF((String) data);
    }

    @Override
    public void writeThrowable(Object obj) throws IOException {
        if (obj instanceof Throwable && !(obj instanceof MessageLite)) {
            obj = ProtobufUtils.convertToThrowableProto((Throwable) obj);
        }
        ProtobufUtils.serialize(obj, os);
        os.flush();
    }

    @Override
    public void writeAttachments(Map<String, Object> attachments) throws IOException {
        if (attachments == null) {
            return;
        }

        Map<String, String> stringAttachments = new HashMap<>();
        attachments.forEach((k, v) -> stringAttachments.put(k, (String) v));

        ProtobufUtils.serialize(MapValue.Map.newBuilder().putAllAttachments(stringAttachments).build(), os);
        os.flush();
    }

    @Override
    public void flushBuffer() throws IOException {
        os.flush();
    }

}