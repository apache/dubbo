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
package org.apache.dubbo.remoting.websocket.netty4;

import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.LimitedByteBufOutputStream;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessageFrame;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import io.netty.channel.Channel;

public class NettyWebSocketChannel implements H2StreamChannel {

    private final Channel channel;

    private final TripleConfig tripleConfig;

    public NettyWebSocketChannel(Channel channel, TripleConfig tripleConfig) {
        this.channel = channel;
        this.tripleConfig = tripleConfig;
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        NettyHttpChannelFutureListener futureListener = new NettyHttpChannelFutureListener();
        channel.close().addListener(futureListener);
        return futureListener;
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        return new Http2OutputMessageFrame(
                new LimitedByteBufOutputStream(
                        channel.alloc().buffer(), tripleConfig.getMaxResponseBodySizeOrDefault()),
                endStream);
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        NettyHttpChannelFutureListener futureListener = new NettyHttpChannelFutureListener();
        channel.write(httpMetadata).addListener(futureListener);
        return futureListener;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        NettyHttpChannelFutureListener futureListener = new NettyHttpChannelFutureListener();
        channel.write(httpOutputMessage).addListener(futureListener);
        return futureListener;
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public void flush() {
        channel.flush();
    }
}
