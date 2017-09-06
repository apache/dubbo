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
package com.alibaba.dubbo.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * UnsafeByteArrayInputStrem.
 *
 * @author qian.lei
 */

public class UnsafeByteArrayInputStream extends InputStream {
    protected byte mData[];

    protected int mPosition, mLimit, mMark = 0;

    public UnsafeByteArrayInputStream(byte buf[]) {
        this(buf, 0, buf.length);
    }

    public UnsafeByteArrayInputStream(byte buf[], int offset) {
        this(buf, offset, buf.length - offset);
    }

    public UnsafeByteArrayInputStream(byte buf[], int offset, int length) {
        mData = buf;
        mPosition = mMark = offset;
        mLimit = Math.min(offset + length, buf.length);
    }

    public int read() {
        return (mPosition < mLimit) ? (mData[mPosition++] & 0xff) : -1;
    }

    public int read(byte b[], int off, int len) {
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || len > b.length - off)
            throw new IndexOutOfBoundsException();
        if (mPosition >= mLimit)
            return -1;
        if (mPosition + len > mLimit)
            len = mLimit - mPosition;
        if (len <= 0)
            return 0;
        System.arraycopy(mData, mPosition, b, off, len);
        mPosition += len;
        return len;
    }

    public long skip(long len) {
        if (mPosition + len > mLimit)
            len = mLimit - mPosition;
        if (len <= 0)
            return 0;
        mPosition += len;
        return len;
    }

    public int available() {
        return mLimit - mPosition;
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int readAheadLimit) {
        mMark = mPosition;
    }

    public void reset() {
        mPosition = mMark;
    }

    public void close() throws IOException {
    }

    public int position() {
        return mPosition;
    }

    public void position(int newPosition) {
        mPosition = newPosition;
    }

    public int size() {
        return mData == null ? 0 : mData.length;
    }
}