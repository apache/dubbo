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
package org.apache.dubbo.rpc.protocol.tri.test;

import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

public class MockH2StreamChannel implements H2StreamChannel {

    private HttpMetadata httpMetadata;
    private final List<ByteBuf> bodies = new ArrayList<>();

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        if (this.httpMetadata == null) {
            this.httpMetadata = httpMetadata;
        } else {
            this.httpMetadata.headers().add(httpMetadata.headers());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        bodies.add(httpOutputMessage.getBody());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> sendMessage(Object message, boolean endStream) {
        return writeMessage((HttpOutputMessage) message);
    }

    @Override
    public SocketAddress remoteAddress() {
        return InetSocketAddress.createUnresolved(TestProtocol.HOST, TestProtocol.PORT + 1);
    }

    @Override
    public SocketAddress localAddress() {
        return InetSocketAddress.createUnresolved(TestProtocol.HOST, TestProtocol.PORT);
    }

    @Override
    public void flush() {}

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public HttpOutputMessage newOutputMessage() {
        return new MockHttp2OutputMessage(false);
    }

    @Override
    public HttpOutputMessage newOutputMessage(ByteBuf body) {
        return new MockHttp2OutputMessage(false);
    }

    @Override
    public ByteBufAllocator alloc() {
        return UnpooledByteBufAllocator.DEFAULT;
    }

    public HttpMetadata getHttpMetadata() {
        return httpMetadata;
    }

    public List<ByteBuf> getBodies() {
        return bodies;
    }
}
