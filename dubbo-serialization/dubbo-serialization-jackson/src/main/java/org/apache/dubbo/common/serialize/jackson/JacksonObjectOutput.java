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
package org.apache.dubbo.common.serialize.jackson;

import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.jackson.utils.JacksonUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Jackson object output implementation
 */
public class JacksonObjectOutput implements ObjectOutput {

    private final PrintWriter writer;

    public JacksonObjectOutput(OutputStream out) {
        this(new OutputStreamWriter(out));
    }

    public JacksonObjectOutput(Writer writer) {
        this.writer = new PrintWriter(writer);
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
        writer.println(new String(b));
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        writer.println(new String(b, off, len));
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        char[] json = JacksonUtils.writeValueAsString(obj).toCharArray();
        writer.write(json, 0, json.length);
        writer.println();
        writer.flush();
        json = null;
    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
    }

    @Override
    public void writeThrowable(Object obj) throws IOException {
        writeObjectClass(obj);
    }

    @Override
    public void writeEvent(Object data) throws IOException {
        writeObjectClass(data);
    }

    protected void writeObjectClass(Object data) throws JsonProcessingException {
        String value;
        if (JacksonUtils.isArray(data)) {
            value = JacksonUtils.writeValueAsString(data);
        } else {
            value = "{\"@c\":\"" + data.getClass().getName() + "\","
                    + JacksonUtils.writeValueAsString(data).substring(1);
        }
        char[] json = value.toCharArray();
        writer.write(json, 0, json.length);
        writer.println();
        writer.flush();
        json = null;
        value = null;
    }

}
