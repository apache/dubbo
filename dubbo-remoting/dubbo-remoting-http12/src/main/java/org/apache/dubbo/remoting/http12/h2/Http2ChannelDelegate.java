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

import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class Http2ChannelDelegate implements H2StreamChannel {

    private final H2StreamChannel h2StreamChannel;

    public Http2ChannelDelegate(H2StreamChannel h2StreamChannel) {
        this.h2StreamChannel = h2StreamChannel;
    }

    public H2StreamChannel getH2StreamChannel() {
        return h2StreamChannel;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        return h2StreamChannel.writeHeader(httpMetadata);
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        return h2StreamChannel.writeMessage(httpOutputMessage);
    }

    @Override
    public SocketAddress remoteAddress() {
        return h2StreamChannel.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return h2StreamChannel.localAddress();
    }

    @Override
    public void flush() {
        h2StreamChannel.flush();
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        return h2StreamChannel.writeResetFrame(errorCode);
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        return h2StreamChannel.newOutputMessage(endStream);
    }

    @Override
    public String toString() {
        return "Http2ChannelDelegate{" + "h2StreamChannel=" + h2StreamChannel + '}';
    }
}
