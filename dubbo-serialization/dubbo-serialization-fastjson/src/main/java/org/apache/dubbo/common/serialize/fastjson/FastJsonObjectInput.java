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
package org.apache.dubbo.common.serialize.fastjson;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.utils.PojoUtils;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

public class FastJsonObjectInput implements ObjectInput {

    private final BufferedReader reader;

    public FastJsonObjectInput(InputStream in) {
        this(new InputStreamReader(in));
    }

    public FastJsonObjectInput(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    @Override
    public boolean readBool() throws IOException {
        return this.read(boolean.class);
    }

    @Override
    public byte readByte() throws IOException {
        return this.read(byte.class);
    }

    @Override
    public short readShort() throws IOException {
        return this.read(short.class);
    }

    @Override
    public int readInt() throws IOException {
        return this.read(int.class);
    }

    @Override
    public long readLong() throws IOException {
        return this.read(long.class);
    }

    @Override
    public float readFloat() throws IOException {
        return this.read(float.class);
    }

    @Override
    public double readDouble() throws IOException {
        return this.read(double.class);
    }

    @Override
    public String readUTF() throws IOException {
        return this.read(String.class);
    }

    @Override
    public byte[] readBytes() throws IOException {
        return this.readJSONString().getBytes();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        String json = this.readJSONString();
        return JSON.parse(json);
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return this.read(cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        Object value = this.readObject(cls);
        return (T) PojoUtils.realize(value, cls, type);
    }

    private String readJSONString() throws IOException, EOFException {
        StringBuilder builder = new StringBuilder();

        String line;
        while((line = reader.readLine()) != null) {
            builder.append(line);
        }

        if (builder.length() == 0) {
            throw new EOFException();
        }
        return builder.toString();
    }

    private <T> T read(Class<T> cls) throws IOException {
        String json = this.readJSONString();
        return JSON.parseObject(json, cls);
    }
}
