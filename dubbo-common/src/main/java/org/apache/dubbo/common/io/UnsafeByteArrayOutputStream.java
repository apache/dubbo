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
package org.apache.dubbo.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * UnsafeByteArrayOutputStream.
 */
public class UnsafeByteArrayOutputStream extends OutputStream {
    protected byte[] mBuffer;

    protected int mCount;

    public UnsafeByteArrayOutputStream() {
        this(32);
    }

    public UnsafeByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        mBuffer = new byte[size];
    }

    @Override
    public void write(int b) {
        int newcount = mCount + 1;
        if (newcount > mBuffer.length) {
            mBuffer = Bytes.copyOf(mBuffer, Math.max(mBuffer.length << 1, newcount));
        }
        mBuffer[mCount] = (byte) b;
        mCount = newcount;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        int newcount = mCount + len;
        if (newcount > mBuffer.length) {
            mBuffer = Bytes.copyOf(mBuffer, Math.max(mBuffer.length << 1, newcount));
        }
        System.arraycopy(b, off, mBuffer, mCount, len);
        mCount = newcount;
    }

    public int size() {
        return mCount;
    }

    public void reset() {
        mCount = 0;
    }

    public byte[] toByteArray() {
        return Bytes.copyOf(mBuffer, mCount);
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(mBuffer, 0, mCount);
    }

    public void writeTo(OutputStream out) throws IOException {
        out.write(mBuffer, 0, mCount);
    }

    @Override
    public String toString() {
        return new String(mBuffer, 0, mCount);
    }

    public String toString(String charset) throws UnsupportedEncodingException {
        return new String(mBuffer, 0, mCount, charset);
    }

    @Override
    public void close() throws IOException {
    }
}
