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
package org.apache.dubbo.serialize.hessian;

import com.caucho.hessian.io.Hessian2Output;
import org.apache.dubbo.common.serialize.ObjectOutput;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Hessian2 Object output.
 */
public class Hessian2ObjectOutput implements ObjectOutput {
    private final Hessian2Output output;

    public Hessian2ObjectOutput(OutputStream os) {
        output = new Hessian2Output(os);
        output.setSerializerFactory(Hessian2SerializerFactory.INSTANCE);
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        output.writeDouble(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        output.writeBytes(b);
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        output.writeBytes(b, off, len);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        output.writeString(v);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        output.writeObject(obj);
    }

    @Override
    public void flushBuffer() throws IOException {
        output.flushBuffer();
    }
}
