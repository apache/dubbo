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

import com.caucho.hessian.io.Hessian2Input;
import org.apache.dubbo.common.serialize.ObjectInput;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Hessian2 Object input.
 */
public class Hessian2ObjectInput implements ObjectInput {
    private final Hessian2Input input;

    public Hessian2ObjectInput(InputStream is) {
        input = new Hessian2Input(is);
        input.setSerializerFactory(Hessian2SerializerFactory.INSTANCE);
    }

    @Override
    public boolean readBool() throws IOException {
        return input.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) input.readInt();
    }

    @Override
    public short readShort() throws IOException {
        return (short) input.readInt();
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return input.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return (float) input.readDouble();
    }

    @Override
    public double readDouble() throws IOException {
        return input.readDouble();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return input.readBytes();
    }

    @Override
    public String readUTF() throws IOException {
        return input.readString();
    }

    @Override
    public Object readObject() throws IOException {
        return input.readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException {
        return (T) input.readObject(cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException {
        return readObject(cls);
    }

}
