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
package org.apache.dubbo.common.serialize.avro;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.util.Utf8;
import org.apache.dubbo.common.serialize.ObjectInput;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AvroObjectInput implements ObjectInput {
    private static DecoderFactory decoderFactory = DecoderFactory.get();
    private BinaryDecoder decoder;

    public AvroObjectInput(InputStream in) {
        decoder = decoderFactory.binaryDecoder(in, null);
    }

    @Override
    public boolean readBool() throws IOException {
        return decoder.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        byte[] bytes = new byte[1];
        decoder.readFixed(bytes);
        return bytes[0];
    }

    @Override
    public short readShort() throws IOException {
        return (short) decoder.readInt();
    }

    @Override
    public int readInt() throws IOException {
        return decoder.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return decoder.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return decoder.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return decoder.readDouble();
    }

    @Override
    public String readUTF() throws IOException {
        Utf8 result = new Utf8();
        result = decoder.readString(result);
        return result.toString();
    }

    @Override
    public byte[] readBytes() throws IOException {
        String resultStr = decoder.readString();
        return resultStr.getBytes("utf8");
    }

    /**
     * will lost all attribute
     */
    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        ReflectDatumReader<Object> reader = new ReflectDatumReader<>(Object.class);
        return reader.read(null, decoder);
    }

    @Override
    @SuppressWarnings(value = {"unchecked"})
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        //Map interface class change to HashMap implement
        if (cls == Map.class) {
            cls = (Class<T>) HashMap.class;
        }

        ReflectDatumReader<T> reader = new ReflectDatumReader<>(cls);
        return reader.read(null, decoder);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        ReflectDatumReader<T> reader = new ReflectDatumReader<>(cls);
        return reader.read(null, decoder);
    }

}
