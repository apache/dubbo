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

import org.apache.dubbo.remoting.http12.HttpOutputMessage;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public final class Http1OutputMessage implements HttpOutputMessage {

    private final ByteBuf body;
    private final ByteBufAllocator allocator;

    public Http1OutputMessage(ByteBuf body) {
        this.body = body;
        this.allocator = body.alloc();
    }

    public Http1OutputMessage(ByteBufAllocator allocator) {
        this.allocator = allocator;
        this.body = allocator.buffer();
    }

    @Override
    public ByteBuf getBody() {
        return body;
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return allocator;
    }

    @Override
    public void close() throws IOException {
        if (body != null && body.refCnt() > 0) {
            body.release();
        }
    }
}
