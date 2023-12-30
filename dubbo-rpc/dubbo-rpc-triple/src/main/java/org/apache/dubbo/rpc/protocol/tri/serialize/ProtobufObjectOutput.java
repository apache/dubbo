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

import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.rpc.protocol.tri.SingleProtobufUtils;

import java.io.IOException;
import java.io.OutputStream;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;

public class ProtobufObjectOutput implements ObjectOutput {

    private final OutputStream output;

    public ProtobufObjectOutput(OutputStream output) {
        this.output = output;
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        SingleProtobufUtils.serialize(obj, output);
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        output.write(BoolValue.newBuilder().setValue(v).build().toByteArray());
    }

    @Override
    public void writeByte(byte v) throws IOException {
        output.write(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        output.write(Int32Value.newBuilder().setValue(v).build().toByteArray());
    }

    @Override
    public void writeInt(int v) throws IOException {
        output.write(Int32Value.newBuilder().setValue(v).build().toByteArray());
    }

    @Override
    public void writeLong(long v) throws IOException {
        output.write(Int64Value.newBuilder().setValue(v).build().toByteArray());
    }

    @Override
    public void writeFloat(float v) throws IOException {
        output.write(FloatValue.newBuilder().setValue(v).build().toByteArray());
    }

    @Override
    public void writeDouble(double v) throws IOException {
        output.write(DoubleValue.newBuilder().setValue(v).build().toByteArray());
    }

    @Override
    public void writeUTF(String v) throws IOException {
        output.write(StringValue.newBuilder().setValue(v).build().toByteArray());
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        output.write(
                BytesValue.newBuilder().setValue(ByteString.copyFrom(v)).build().toByteArray());
    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {
        output.write(BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(v, off, len))
                .build()
                .toByteArray());
    }

    @Override
    public void flushBuffer() throws IOException {
        output.flush();
    }
}
