/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.serialize.support.json;

import com.alibaba.dubbo.common.json.Jackson;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * jackson object output
 *
 * @author dylan
 */
public class JacksonObjectOutput implements ObjectOutput {

    private final ObjectMapper objectMapper;
    private final Map<String, Object> data;
    private final static String KEY_PREFIX = "$";
    private int index = 0;

    private final PrintWriter writer;

    public JacksonObjectOutput(OutputStream out) {
        this(new OutputStreamWriter(out));
    }

    public JacksonObjectOutput(Writer writer) {
        this.objectMapper = Jackson.getObjectMapper();
        this.writer = new PrintWriter(writer);
        this.data = new HashMap<String, Object>();
    }

    public void writeBool(boolean v) throws IOException {
        writeObject0(v);
    }

    public void writeByte(byte v) throws IOException {
        writeObject0(v);
    }

    public void writeShort(short v) throws IOException {
        writeObject0(v);
    }

    public void writeInt(int v) throws IOException {
        writeObject0(v);
    }

    public void writeLong(long v) throws IOException {
        writeObject0(v);
    }

    public void writeFloat(float v) throws IOException {
        writeObject0(v);
    }

    public void writeDouble(double v) throws IOException {
        writeObject0(v);
    }

    public void writeUTF(String v) throws IOException {
        writeObject0(v);
    }

    public void writeBytes(byte[] b) throws IOException {
        writeObject0(new String(b));
    }

    public void writeBytes(byte[] b, int off, int len) throws IOException {
        writeObject0(new String(b, off, len));
    }

    public void writeObject(Object obj) throws IOException {
//        int i = ++index;
        if (obj == null) {
            writeObject0(obj);
            return;
        }
        //write data value
        writeObject0(obj);
        //write data type
        Class c = obj.getClass();
        String desc = ReflectUtils.getDesc(c);
        data.put(KEY_PREFIX + (index) + "t", desc);
//        if (obj instanceof Collection) {
//            //集合类型
//        } else if (obj instanceof Map) {
//            //
//        } else {
//        }
    }

    private void writeObject0(Object obj) throws IOException {
        data.put(KEY_PREFIX + (++index), objectMapper.writeValueAsString(obj));
    }

    public void flushBuffer() throws IOException {
        objectMapper.writeValue(writer, data);
        writer.println();
        writer.flush();
    }

}