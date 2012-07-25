/*
 * Copyright 1999-2012 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.common.serialize.support.nativejava;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.utils.Assert;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class NativeJavaObjectOutput implements ObjectOutput {

    private final ObjectOutputStream outputStream;

    public NativeJavaObjectOutput(OutputStream os) throws IOException {
        this(new ObjectOutputStream(os));
    }

    protected NativeJavaObjectOutput(ObjectOutputStream out) {
        Assert.notNull(out, "output == null");
        this.outputStream = out;
    }

    protected ObjectOutputStream getObjectOutputStream() {
        return outputStream;
    }

    public void writeObject(Object obj) throws IOException {
        outputStream.writeObject(obj);
    }

    public void writeBool(boolean v) throws IOException {
        outputStream.writeBoolean(v);
    }

    public void writeByte(byte v) throws IOException {
        outputStream.writeByte(v);
    }

    public void writeShort(short v) throws IOException {
        outputStream.writeShort(v);
    }

    public void writeInt(int v) throws IOException {
        outputStream.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        outputStream.writeLong(v);
    }

    public void writeFloat(float v) throws IOException {
        outputStream.writeFloat(v);
    }

    public void writeDouble(double v) throws IOException {
        outputStream.writeDouble(v);
    }

    public void writeUTF(String v) throws IOException {
        outputStream.writeUTF(v);
    }

    public void writeBytes(byte[] v) throws IOException {
        if (v == null) {
            outputStream.writeInt(-1);
        } else {
            writeBytes(v, 0, v.length);
        }
    }

    public void writeBytes(byte[] v, int off, int len) throws IOException {
        if (v == null) {
            outputStream.writeInt(-1);
        } else {
            outputStream.writeInt(len);
            outputStream.write(v, off, len);
        }
    }

    public void flushBuffer() throws IOException {
        outputStream.flush();
    }
}
