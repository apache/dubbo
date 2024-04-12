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
package org.apache.dubbo.remoting.http12.netty4.h1;

import org.apache.dubbo.config.TripleConfig;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h1.Http1Channel;
import org.apache.dubbo.remoting.http12.h1.Http1OutputMessage;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpMessage;

public class NettyHttp1Channel implements Http1Channel {

    private final Channel channel;

    private final TripleConfig triple;

    public NettyHttp1Channel(Channel channel, TripleConfig triple) {
        this.channel = channel;
        this.triple = triple;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        this.channel.writeAndFlush(httpMetadata).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        this.channel.writeAndFlush(httpOutputMessage).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public CompletableFuture<Void> writeFullHttpMessage(FullHttpMessage fullHttpMessage) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        this.channel.writeAndFlush(fullHttpMessage).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public HttpOutputMessage newOutputMessage() {
        return new Http1OutputMessage(
                new ByteBufOutputStream(channel.alloc().buffer()), triple.getMaxResponseBodySize());
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
    public void flush() {}
}
