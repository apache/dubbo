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
package org.apache.dubbo.rpc.protocol.thrift.io;

import org.apache.dubbo.common.io.Bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
@Deprecated
public class RandomAccessByteArrayOutputStream extends OutputStream {

    protected byte buffer[];

    protected int count;

    public RandomAccessByteArrayOutputStream() {

        this(32);
    }

    public RandomAccessByteArrayOutputStream(int size) {

        if (size < 0)
            throw new IllegalArgumentException("Negative initial size: " + size);
        buffer = new byte[size];
    }

    @Override
    public void write(int b) {

        int newcount = count + 1;
        if (newcount > buffer.length)
            buffer = Bytes.copyOf(buffer, Math.max(buffer.length << 1, newcount));
        buffer[count] = (byte) b;
        count = newcount;
    }

    @Override
    public void write(byte b[], int off, int len) {

        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
            throw new IndexOutOfBoundsException();
        if (len == 0)
            return;
        int newcount = count + len;
        if (newcount > buffer.length)
            buffer = Bytes.copyOf(buffer, Math.max(buffer.length << 1, newcount));
        System.arraycopy(b, off, buffer, count, len);
        count = newcount;
    }

    public int size() {

        return count;
    }

    public void setWriteIndex(int index) {
        count = index;
    }

    public void reset() {

        count = 0;
    }

    public byte[] toByteArray() {

        return Bytes.copyOf(buffer, count);
    }

    public ByteBuffer toByteBuffer() {

        return ByteBuffer.wrap(buffer, 0, count);
    }

    public void writeTo(OutputStream out) throws IOException {

        out.write(buffer, 0, count);
    }

    @Override
    public String toString() {

        return new String(buffer, 0, count);
    }

    public String toString(String charset) throws UnsupportedEncodingException {

        return new String(buffer, 0, count, charset);
    }

    @Override
    public void close() throws IOException {
    }

}
