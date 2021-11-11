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

package org.apache.dubbo.common.serialize.msgpack;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.utils.PojoUtils;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class MsgpackObjectInput implements ObjectInput {

    private final InputStream in;

    private ObjectMapper om;

    public MsgpackObjectInput(InputStream in) {
        this.in = in;
        om = new ObjectMapper(new MessagePackFactory());
        om.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    @Override
    public boolean readBool() throws IOException {
        return read(boolean.class);
    }

    @Override
    public byte readByte() throws IOException {
        return read(byte.class);
    }

    @Override
    public short readShort() throws IOException {
        return read(short.class);
    }

    @Override
    public int readInt() throws IOException {
        return read(int.class);
    }

    @Override
    public long readLong() throws IOException {
        return read(long.class);
    }

    @Override
    public float readFloat() throws IOException {
        return read(float.class);
    }

    @Override
    public double readDouble() throws IOException {
        return read(double.class);
    }

    @Override
    public String readUTF() throws IOException {
        return read(String.class);
    }

    @Override
    public byte[] readBytes() throws IOException {
        return read(byte[].class);
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return om.readValue(this.in, Object.class);
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return read(cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        Object value = readObject(cls);
        return (T) PojoUtils.realize(value, cls, type);
    }

    private <T> T read(Class<T> cls) throws IOException {
        return om.readValue(this.in, cls);
    }

    @Override
    public Throwable readThrowable() throws IOException, ClassNotFoundException {
        Class clazz = readObject(Class.class);
        return (Throwable) readObject(clazz);
    }
}
