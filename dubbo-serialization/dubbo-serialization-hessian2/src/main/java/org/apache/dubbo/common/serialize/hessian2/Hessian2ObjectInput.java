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
package org.apache.dubbo.common.serialize.hessian2;

import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.hessian2.dubbo.Hessian2FactoryInitializer;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Hessian2 object input implementation
 */
public class Hessian2ObjectInput implements ObjectInput, Cleanable {
    private final Hessian2Input mH2i;
    private final Hessian2FactoryInitializer hessian2FactoryInitializer;

    public Hessian2ObjectInput(InputStream is) {
        mH2i = new Hessian2Input(is);
        hessian2FactoryInitializer = Hessian2FactoryInitializer.getInstance();
        mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
    }

    @Override
    public boolean readBool() throws IOException {
        return mH2i.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) mH2i.readInt();
    }

    @Override
    public short readShort() throws IOException {
        return (short) mH2i.readInt();
    }

    @Override
    public int readInt() throws IOException {
        return mH2i.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return mH2i.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return (float) mH2i.readDouble();
    }

    @Override
    public double readDouble() throws IOException {
        return mH2i.readDouble();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return mH2i.readBytes();
    }

    @Override
    public String readUTF() throws IOException {
        return mH2i.readString();
    }

    @Override
    public Object readObject() throws IOException {
        if (!mH2i.getSerializerFactory().getClassLoader().equals(Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
        }
        return mH2i.readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException,
            ClassNotFoundException {
        if (!mH2i.getSerializerFactory().getClassLoader().equals(Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
        }
        return (T) mH2i.readObject(cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        if (!mH2i.getSerializerFactory().getClassLoader().equals(Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
        }
        return readObject(cls);
    }

    public InputStream readInputStream() throws IOException {
        return mH2i.readInputStream();
    }

    @Override
    public void cleanup() {
        if(mH2i != null) {
            mH2i.reset();
        }
    }
}
