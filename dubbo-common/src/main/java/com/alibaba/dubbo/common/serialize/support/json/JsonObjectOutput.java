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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.serialize.ObjectOutput;

/**
 * JsonObjectOutput
 * 
 * @author william.liangf
 */
public class JsonObjectOutput implements ObjectOutput {
    
    private final PrintWriter writer;
    
    private final boolean writeClass;
    
    public JsonObjectOutput(OutputStream out) {
        this(new OutputStreamWriter(out), false);
    }
    
    public JsonObjectOutput(Writer writer) {
        this(writer, false);
    }
    
    public JsonObjectOutput(OutputStream out, boolean writeClass) {
        this(new OutputStreamWriter(out), writeClass);
    }
    
    public JsonObjectOutput(Writer writer, boolean writeClass) {
        this.writer = new PrintWriter(writer);
        this.writeClass = writeClass;
    }

    public void writeBool(boolean v) throws IOException {
        writeObject(v);
    }

    public void writeByte(byte v) throws IOException {
        writeObject(v);
    }

    public void writeShort(short v) throws IOException {
        writeObject(v);
    }

    public void writeInt(int v) throws IOException {
        writeObject(v);
    }

    public void writeLong(long v) throws IOException {
        writeObject(v);
    }

    public void writeFloat(float v) throws IOException {
        writeObject(v);
    }

    public void writeDouble(double v) throws IOException {
        writeObject(v);
    }

    public void writeUTF(String v) throws IOException {
        writeObject(v);
    }

    public void writeBytes(byte[] b) throws IOException {
        writer.println(new String(b));
    }

    public void writeBytes(byte[] b, int off, int len) throws IOException {
        writer.println(new String(b, off, len));
    }

    public void writeObject(Object obj) throws IOException {
        JSON.json(obj, writer, writeClass);
        writer.println();
        writer.flush();
    }

    public void flushBuffer() throws IOException {
        writer.flush();
    }

}