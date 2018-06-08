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

package com.alibaba.dubbo.common.serialize.nativejava;

import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.utils.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;

public class NativeJavaObjectInput implements ObjectInput {

    private final ObjectInputStream inputStream;

    public NativeJavaObjectInput(InputStream is) throws IOException {
        this(new ObjectInputStream(is));
    }

    protected NativeJavaObjectInput(ObjectInputStream is) {
        Assert.notNull(is, "input == null");
        inputStream = is;
    }

    protected ObjectInputStream getObjectInputStream() {
        return inputStream;
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return inputStream.readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

    @Override
    public boolean readBool() throws IOException {
        return inputStream.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return inputStream.readByte();
    }

    @Override
    public short readShort() throws IOException {
        return inputStream.readShort();
    }

    @Override
    public int readInt() throws IOException {
        return inputStream.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return inputStream.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return inputStream.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return inputStream.readDouble();
    }

    @Override
    public String readUTF() throws IOException {
        return inputStream.readUTF();
    }

    @Override
    public byte[] readBytes() throws IOException {
        int len = inputStream.readInt();
        if (len < 0) {
            return null;
        } else if (len == 0) {
            return new byte[]{};
        } else {
            byte[] result = new byte[len];
            inputStream.readFully(result);
            return result;
        }
    }
}
