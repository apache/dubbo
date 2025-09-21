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
package org.apache.dubbo.remoting.http12.h2;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public final class Http2OutputMessageFrame implements Http2OutputMessage {

    private final ByteBuf bodyBuffer;
    private final boolean endStream;
    private final ByteBufAllocator allocator;

    public Http2OutputMessageFrame(ByteBuf bodyBuffer, boolean endStream) {
        this.bodyBuffer = bodyBuffer;
        this.endStream = endStream;
        this.allocator = bodyBuffer != null ? bodyBuffer.alloc() : ByteBufAllocator.DEFAULT;
    }

    public Http2OutputMessageFrame(ByteBufAllocator allocator, boolean endStream) {
        this.allocator = allocator;
        this.bodyBuffer = allocator.buffer();
        this.endStream = endStream;
    }

    @Override
    public ByteBuf getBody() {
        return bodyBuffer;
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return allocator;
    }

    @Override
    public void close() throws IOException {
        if (bodyBuffer != null && bodyBuffer.refCnt() > 0) {
            bodyBuffer.release();
        }
    }

    @Override
    public boolean isEndStream() {
        return endStream;
    }
}
