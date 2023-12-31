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
package org.apache.dubbo.rpc.protocol.tri.serialize;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.rpc.protocol.tri.SingleProtobufUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;

public class ProtobufObjectInput implements ObjectInput {

    private final InputStream input;

    public ProtobufObjectInput(InputStream input) {
        this.input = input;
    }

    @Override
    public Object readObject() {
        throw new UnsupportedOperationException("Protobuf serialization does not support readObject()");
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException {
        return SingleProtobufUtils.deserialize(input, cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException {
        return SingleProtobufUtils.deserialize(input, cls);
    }

    @Override
    public boolean readBool() throws IOException {
        return BoolValue.parseFrom(input).getValue();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) input.read();
    }

    @Override
    public short readShort() throws IOException {
        return (short) Int32Value.parseFrom(input).getValue();
    }

    @Override
    public int readInt() throws IOException {
        return Int32Value.parseFrom(input).getValue();
    }

    @Override
    public long readLong() throws IOException {
        return Int64Value.parseFrom(input).getValue();
    }

    @Override
    public float readFloat() throws IOException {
        return FloatValue.parseFrom(input).getValue();
    }

    @Override
    public double readDouble() throws IOException {
        return DoubleValue.parseFrom(input).getValue();
    }

    @Override
    public String readUTF() throws IOException {
        return StringValue.parseFrom(input).getValue();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return BytesValue.parseFrom(input).getValue().toByteArray();
    }
}
