/*
 * Copyright 1999-2011 Alibaba Group.
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

package com.alibaba.dubbo.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public final class CountInputStream extends InputStream{

    private InputStream inputStream;
    private long readBytes;

    public CountInputStream(InputStream inputStream) {
        super();
        this.inputStream = inputStream;
    }

    public long getReadBytes() {
        return readBytes;
    }

    @Override
    public int read() throws IOException {
        int result = inputStream.read();
        if (result != -1) {
            ++readBytes;
        }
        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = inputStream.read(b);
        if (result != -1) {
            readBytes += result;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = inputStream.read(b, off, len);
        if (result != -1) {
            readBytes += result;
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        long result = inputStream.skip(n);
        if (result > 0) {
            readBytes += result;
        }
        return result;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}
