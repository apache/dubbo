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
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockH2StreamChannel implements H2StreamChannel {

    private HttpMetadata httpMetadata;
    private final List<OutputStream> bodies = new ArrayList<>();

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        this.httpMetadata = httpMetadata;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        bodies.add(httpOutputMessage.getBody());
        return CompletableFuture.completedFuture(null);
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
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        return new MockHttp2OutputMessage(endStream);
    }

    public HttpMetadata getHttpMetadata() {
        return httpMetadata;
    }

    public List<OutputStream> getBodies() {
        return bodies;
    }
}
