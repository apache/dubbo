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
package org.apache.dubbo.common.serialize.java;

import org.apache.dubbo.common.serialize.nativejava.NativeJavaObjectInput;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;

/**
 * Java object input implementation
 */
public class JavaObjectInput extends NativeJavaObjectInput {
    public final static int MAX_BYTE_ARRAY_LENGTH = 8 * 1024 * 1024;

    public JavaObjectInput(InputStream is) throws IOException {
        super(new ObjectInputStream(is));
    }

    public JavaObjectInput(InputStream is, boolean compacted) throws IOException {
        super(compacted ? new CompactedObjectInputStream(is) : new ObjectInputStream(is));
    }

    @Override
    public byte[] readBytes() throws IOException {
        int len = getObjectInputStream().readInt();
        if (len < 0) {
            return null;
        }
        if (len == 0) {
            return new byte[0];
        }
        if (len > MAX_BYTE_ARRAY_LENGTH) {
            throw new IOException("Byte array length too large. " + len);
        }

        byte[] b = new byte[len];
        getObjectInputStream().readFully(b);
        return b;
    }

    @Override
    public String readUTF() throws IOException {
        int len = getObjectInputStream().readInt();
        if (len < 0) {
            return null;
        }

        return getObjectInputStream().readUTF();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        byte b = getObjectInputStream().readByte();
        if (b == 0) {
            return null;
        }

        return getObjectInputStream().readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException,
            ClassNotFoundException {
        return (T) readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

}
