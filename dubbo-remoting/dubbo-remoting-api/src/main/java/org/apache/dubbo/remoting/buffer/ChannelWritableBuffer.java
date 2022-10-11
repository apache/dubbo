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

package org.apache.dubbo.remoting.buffer;

import io.netty.buffer.ByteBuf;

/**
 * The {@link WritableBuffer} used by the Netty transport.
 */
public class ChannelWritableBuffer implements WritableBuffer {

    private final ByteBuf bytebuf;

    ChannelWritableBuffer(ByteBuf bytebuf) {
        this.bytebuf = bytebuf;
    }

    @Override
    public void write(byte[] src, int srcIndex, int length) {
        bytebuf.writeBytes(src, srcIndex, length);
    }

    @Override
    public void write(byte b) {
        bytebuf.writeByte(b);
    }

    @Override
    public void writeInt(int value) {
        bytebuf.writeInt(value);
    }

    @Override
    public int writableBytes() {
        return bytebuf.writableBytes();
    }

    @Override
    public int readableBytes() {
        return bytebuf.readableBytes();
    }

    @Override
    public void release() {
        bytebuf.release();
    }

    public ByteBuf bytebuf() {
        return bytebuf;
    }
}
