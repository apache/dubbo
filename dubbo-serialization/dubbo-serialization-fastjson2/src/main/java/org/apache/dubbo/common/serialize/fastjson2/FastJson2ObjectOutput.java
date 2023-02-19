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
package org.apache.dubbo.common.serialize.fastjson2;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.dubbo.common.serialize.ObjectOutput;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;

/**
 * FastJson object output implementation
 */
public class FastJson2ObjectOutput implements ObjectOutput {

    private final Fastjson2CreatorManager fastjson2CreatorManager;

    private final Fastjson2SecurityManager fastjson2SecurityManager;

    private volatile ClassLoader classLoader;
    private final OutputStream os;

    public FastJson2ObjectOutput(Fastjson2CreatorManager fastjson2CreatorManager,
                                 Fastjson2SecurityManager fastjson2SecurityManager,
                                 OutputStream out) {
        this.fastjson2CreatorManager = fastjson2CreatorManager;
        this.fastjson2SecurityManager = fastjson2SecurityManager;
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.os = out;
        fastjson2CreatorManager.setCreator(classLoader);
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        os.write(b.length);
        os.write(b);
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        os.write(len);
        os.write(b, off, len);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        updateClassLoaderIfNeed();
        byte[] bytes;
        if (fastjson2SecurityManager.getSecurityFilter().isCheckSerializable()) {
            bytes = JSONB.toBytes(obj, JSONWriter.Feature.WriteClassName,
                JSONWriter.Feature.FieldBased,
                JSONWriter.Feature.ErrorOnNoneSerializable,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.NotWriteDefaultValue,
                JSONWriter.Feature.NotWriteHashMapArrayListClassName,
                JSONWriter.Feature.WriteNameAsSymbol);
        } else {
            bytes = JSONB.toBytes(obj, JSONWriter.Feature.WriteClassName,
                JSONWriter.Feature.FieldBased,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.NotWriteDefaultValue,
                JSONWriter.Feature.NotWriteHashMapArrayListClassName,
                JSONWriter.Feature.WriteNameAsSymbol);
        }
        writeLength(bytes.length);
        os.write(bytes);
        os.flush();
    }

    private void updateClassLoaderIfNeed() {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        if (currentClassLoader != classLoader) {
            fastjson2CreatorManager.setCreator(currentClassLoader);
            classLoader = currentClassLoader;
        }
    }

    private void writeLength(int value) throws IOException {
        byte[] bytes = new byte[Integer.BYTES];
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            bytes[length - i - 1] = (byte) (value & 0xFF);
            value >>= 8;
        }
        os.write(bytes);
    }

    @Override
    public void flushBuffer() throws IOException {
        os.flush();
    }

}
