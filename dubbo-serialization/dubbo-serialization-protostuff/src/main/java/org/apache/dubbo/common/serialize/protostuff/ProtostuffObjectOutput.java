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

package org.apache.dubbo.common.serialize.protostuff;

import io.protostuff.GraphIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.protostuff.utils.WrapperUtils;

/**
 * Protostuff object output implementation
 */
public class ProtostuffObjectOutput implements ObjectOutput {

    private LinkedBuffer buffer = LinkedBuffer.allocate();
    private DataOutputStream dos;

    public ProtostuffObjectOutput(OutputStream outputStream) {
        dos = new DataOutputStream(outputStream);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeObject(Object obj) throws IOException {

        byte[] bytes;
        byte[] classNameBytes;

        try {
            if (WrapperUtils.needWrapper(obj)) {
                Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
                Wrapper wrapper = new Wrapper(obj);
                bytes = GraphIOUtil.toByteArray(wrapper, schema, buffer);
                classNameBytes = Wrapper.class.getName().getBytes();
            } else {
                Schema schema = RuntimeSchema.getSchema(obj.getClass());
                bytes = GraphIOUtil.toByteArray(obj, schema, buffer);
                classNameBytes = obj.getClass().getName().getBytes();
            }
        } finally {
            buffer.clear();
        }

        dos.writeInt(classNameBytes.length);
        dos.writeInt(bytes.length);
        dos.write(classNameBytes);
        dos.write(bytes);
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        dos.writeBoolean(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        dos.writeByte(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        dos.writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        dos.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        dos.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        dos.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        dos.writeDouble(v);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        byte[] bytes = v.getBytes();
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        dos.writeInt(v.length);
        dos.write(v);
    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {
        dos.writeInt(len);
        byte[] bytes = new byte[len];
        System.arraycopy(v, off, bytes, 0, len);
        dos.write(bytes);
    }

    @Override
    public void flushBuffer() throws IOException {
        dos.flush();
    }
}
