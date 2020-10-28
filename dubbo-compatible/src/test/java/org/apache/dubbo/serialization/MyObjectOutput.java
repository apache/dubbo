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

package org.apache.dubbo.serialization;

import com.alibaba.dubbo.common.serialize.ObjectOutput;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MyObjectOutput implements ObjectOutput {

    private final BufferedWriter writer;

    public MyObjectOutput(OutputStream outputStream) {
        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    @Override
    public void writeObject(Object obj) throws IOException {

    }

    @Override
    public void writeBool(boolean v) throws IOException {

    }

    @Override
    public void writeByte(byte v) throws IOException {

    }

    @Override
    public void writeShort(short v) throws IOException {

    }

    @Override
    public void writeInt(int v) throws IOException {

    }

    @Override
    public void writeLong(long v) throws IOException {

    }

    @Override
    public void writeFloat(float v) throws IOException {

    }

    @Override
    public void writeDouble(double v) throws IOException {

    }

    @Override
    public void writeUTF(String v) throws IOException {
        writer.write(v);
        writer.write('\n');
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {

    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {

    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
    }
}
