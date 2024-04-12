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
package org.apache.dubbo.remoting.http12.h1;

import org.apache.dubbo.remoting.http12.exception.HttpOverPayloadException;

import java.io.IOException;

import io.netty.buffer.ByteBufOutputStream;

public class LimitedByteBufOutputStreamDelegate extends ByteBufOutputStream {

    private final ByteBufOutputStream outputStream;

    private final int capacity;

    private int writerIndex;

    public LimitedByteBufOutputStreamDelegate(ByteBufOutputStream outputStream, int capacity) {
        super(outputStream.buffer());
        this.outputStream = outputStream;
        this.capacity = capacity == 0 ? Integer.MAX_VALUE : capacity;
        this.writerIndex = 0;
    }

    @Override
    public void write(int b) throws IOException {
        writerIndex++;
        if (writerIndex > capacity) {
            throw new HttpOverPayloadException(writerIndex, capacity);
        }
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        writerIndex += b.length;
        if (writerIndex > capacity) {
            throw new HttpOverPayloadException(writerIndex, capacity);
        }
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writerIndex += len;
        if (writerIndex > capacity) {
            throw new HttpOverPayloadException(writerIndex, capacity);
        }
        outputStream.write(b, off, len);
    }
}
