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
package org.apache.dubbo.common.serialize;

import org.apache.dubbo.common.URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Map;

public class DefaultSerializationExceptionWrapper implements Serialization {

    private final Serialization serialization;

    public DefaultSerializationExceptionWrapper(Serialization serialization) {
        if (serialization == null) {
            throw new IllegalArgumentException("serialization == null");
        }
        this.serialization = serialization;
    }

    @Override
    public byte getContentTypeId() {
        return serialization.getContentTypeId();
    }

    @Override
    public String getContentType() {
        return serialization.getContentType();
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        ObjectOutput objectOutput = serialization.serialize(url, output);
        return new ProxyObjectOutput(objectOutput);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        ObjectInput objectInput = serialization.deserialize(url, input);
        return new ProxyObjectInput(objectInput);
    }

    static class ProxyObjectInput implements ObjectInput {

        private final ObjectInput target;

        public ProxyObjectInput(ObjectInput target) {
            this.target = target;
        }

        @Override
        public boolean readBool() throws IOException {
            try {
                return target.readBool();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public byte readByte() throws IOException {
            try {
                return target.readByte();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public short readShort() throws IOException {
            try {
                return target.readShort();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public int readInt() throws IOException {
            try {
                return target.readInt();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public long readLong() throws IOException {
            try {
                return target.readLong();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public float readFloat() throws IOException {
            try {
                return target.readFloat();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public double readDouble() throws IOException {
            try {
                return target.readDouble();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public String readUTF() throws IOException {
            try {
                return target.readUTF();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public byte[] readBytes() throws IOException {
            try {
                return target.readBytes();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public Object readObject() throws IOException, ClassNotFoundException {
            try {
                return target.readObject();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
            try {
                return target.readObject(cls);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
            try {
                return target.readObject(cls, type);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public Throwable readThrowable() throws IOException {
            try {
                return target.readThrowable();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public String readEvent() throws IOException {
            try {
                return target.readEvent();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public Map<String, Object> readAttachments() throws IOException, ClassNotFoundException {
            try {
                return target.readAttachments();
            } catch (Exception e) {
                if (e instanceof ClassNotFoundException) {
                    throw e;
                }
                throw handleToIOException(e);
            }
        }
    }

    static class ProxyObjectOutput implements ObjectOutput {

        private final ObjectOutput target;

        public ProxyObjectOutput(ObjectOutput target) {
            this.target = target;
        }

        @Override
        public void writeBool(boolean v) throws IOException {
            try {
                target.writeBool(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeByte(byte v) throws IOException {
            try {
                target.writeByte(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeShort(short v) throws IOException {
            try {
                target.writeShort(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeInt(int v) throws IOException {
            try {
                target.writeInt(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeLong(long v) throws IOException {
            try {
                target.writeLong(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeFloat(float v) throws IOException {
            try {
                target.writeFloat(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeDouble(double v) throws IOException {
            try {
                target.writeDouble(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeUTF(String v) throws IOException {
            try {
                target.writeUTF(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeBytes(byte[] v) throws IOException {
            try {
                target.writeBytes(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeBytes(byte[] v, int off, int len) throws IOException {
            try {
                target.writeBytes(v);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void flushBuffer() throws IOException {
            try {
                target.flushBuffer();
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeObject(Object obj) throws IOException {
            try {
                target.writeObject(obj);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeThrowable(Throwable obj) throws IOException {
            try {
                target.writeThrowable(obj);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeEvent(String data) throws IOException {
            try {
                target.writeEvent(data);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }

        @Override
        public void writeAttachments(Map<String, Object> attachments) throws IOException {
            try {
                target.writeAttachments(attachments);
            } catch (Exception e) {
                throw handleToIOException(e);
            }
        }
    }

    private static IOException handleToIOException(Exception e) {
        if (!(e instanceof IOException)) {
            return new IOException(new SerializationException(e));
        }
        return (IOException) e;
    }
}
