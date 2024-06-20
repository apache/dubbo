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
package org.apache.dubbo.remoting.http3.netty4;

import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessageFrame;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class NettyHttp3StreamChannel implements H2StreamChannel {

    private final QuicStreamChannel http3StreamChannel;

    public NettyHttp3StreamChannel(QuicStreamChannel http3StreamChannel) {
        this.http3StreamChannel = http3StreamChannel;
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        NettyHttpChannelFutureListener futureListener = new NettyHttpChannelFutureListener();
        http3StreamChannel.close().addListener(futureListener);
        return futureListener;
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        ByteBuf buffer = http3StreamChannel.alloc().buffer();
        return new Http2OutputMessageFrame(new ByteBufOutputStream(buffer), endStream);
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        NettyHttpChannelFutureListener futureListener = new NettyHttpChannelFutureListener();
        http3StreamChannel.write(httpMetadata).addListener(futureListener);
        return futureListener;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        NettyHttpChannelFutureListener futureListener = new NettyHttpChannelFutureListener();
        http3StreamChannel.write(httpOutputMessage).addListener(futureListener);
        return futureListener;
    }

    @Override
    public SocketAddress remoteAddress() {
        return http3StreamChannel.parent().remoteSocketAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return http3StreamChannel.parent().localSocketAddress();
    }

    @Override
    public void flush() {
        http3StreamChannel.flush();
    }
}
