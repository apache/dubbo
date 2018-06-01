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
package com.alibaba.dubbo.rpc.protocol.thrift.io;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamWrapper extends InputStream {

    private InputStream is;

    public InputStreamWrapper(InputStream is) {
        if (is == null) {
            throw new NullPointerException("is == null");
        }
        this.is = is;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (is.available() >= b.length) {
            return is.read(b);
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (is.available() >= len) {
            return is.read(b, off, len);
        } else {
            return -1;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        if (is.available() >= 1) {
            return is.read();
        }
        return -1;
    }
}
