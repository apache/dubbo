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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.dubbo.common.serialize.ObjectOutput;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


public class MsgpackObjectOutput implements ObjectOutput {

    private OutputStream out;
    private ObjectMapper om;

    public MsgpackObjectOutput(OutputStream out) {
        this.out = out;
        om = new ObjectMapper(new MessagePackFactory());
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        writeObject(b);
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        byte[] d = Arrays.copyOfRange(b, off, off + len);
        writeObject(d);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        byte[] bytes = om.writeValueAsBytes(obj);
        out.write(bytes);
        out.flush();
        bytes = null;
    }

    @Override
    public void writeThrowable(Object obj) throws IOException {
        writeObject(obj.getClass());
        writeObject(obj);
    }

    @Override
    public void flushBuffer() throws IOException {
        out.flush();
    }

}
