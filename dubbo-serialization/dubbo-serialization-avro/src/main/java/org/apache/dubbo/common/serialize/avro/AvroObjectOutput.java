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

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.dubbo.common.serialize.ObjectOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AvroObjectOutput implements ObjectOutput {
    private static EncoderFactory encoderFactory = EncoderFactory.get();
    private BinaryEncoder encoder;

    public AvroObjectOutput(OutputStream out) {
        encoder = encoderFactory.binaryEncoder(out, null);
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        encoder.writeBoolean(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        encoder.writeFixed(new byte[]{v});
    }

    @Override
    public void writeShort(short v) throws IOException {
        encoder.writeInt(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        encoder.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        encoder.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        encoder.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        encoder.writeDouble(v);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        encoder.writeString(new Utf8(v));
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        encoder.writeString(new String(v, StandardCharsets.UTF_8));
    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {
        byte[] v2 = Arrays.copyOfRange(v, off, off + len);
        encoder.writeString(new String(v2, StandardCharsets.UTF_8));
    }

    @Override
    public void flushBuffer() throws IOException {
        encoder.flush();
    }

    @Override
    @SuppressWarnings(value = {"rawtypes", "unchecked"})
    public void writeObject(Object obj) throws IOException {
        if (obj == null) {
            encoder.writeNull();
            return;
        }
        ReflectDatumWriter dd = new ReflectDatumWriter<>(obj.getClass());
        dd.write(obj, encoder);
    }

}
