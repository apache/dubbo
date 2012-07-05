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
import java.io.OutputStream;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public final class CountOutputStream extends OutputStream {

    private OutputStream outputStream;
    private long writeBytes;

    public CountOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public long getWriteBytes() {
        return writeBytes;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
        ++writeBytes;
    }

    @Override
    public void write(byte[] b) throws IOException {
        outputStream.write(b);
        writeBytes += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
        writeBytes += len;
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
