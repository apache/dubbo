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
package org.apache.dubbo.remoting.http12.netty4.h2;

import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.LimitedByteBufOutputStream;
import org.apache.dubbo.remoting.http12.h2.H2FlowController;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessageFrame;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;

public class NettyH2StreamChannel implements H2StreamChannel {

    private final Http2StreamChannel http2StreamChannel;

    private final TripleConfig tripleConfig;

    private volatile Runnable onWritableHandler;

    public NettyH2StreamChannel(Http2StreamChannel http2StreamChannel, TripleConfig tripleConfig) {
        this.http2StreamChannel = http2StreamChannel;
        this.tripleConfig = tripleConfig;
    }

    /**
     * Get the stream ID of this HTTP/2 stream.
     */
    public int streamId() {
        return http2StreamChannel.stream().id();
    }

    /**
     * Get the H2FlowController from the parent channel.
     */
    private H2FlowController getFlowController() {
        Channel parent = http2StreamChannel.parent();
        if (parent != null) {
            return parent.attr(H2FlowController.KEY).get();
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        // WriteQueue.enqueue header frame
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        http2StreamChannel.write(httpMetadata).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        http2StreamChannel.write(httpOutputMessage).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        ByteBuf buffer = http2StreamChannel.alloc().buffer();
        ByteBufOutputStream outputStream =
                new LimitedByteBufOutputStream(buffer, tripleConfig.getMaxResponseBodySizeOrDefault());
        return new Http2OutputMessageFrame(outputStream, endStream);
    }

    @Override
    public SocketAddress remoteAddress() {
        return this.http2StreamChannel.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return this.http2StreamChannel.localAddress();
    }

    @Override
    public void flush() {
        this.http2StreamChannel.flush();
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        DefaultHttp2ResetFrame resetFrame = new DefaultHttp2ResetFrame(errorCode);
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        http2StreamChannel.write(resetFrame).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public void requestInboundData(int numBytes) {
        H2FlowController flowController = getFlowController();
        if (flowController != null) {
            flowController.consumeBytes(streamId(), numBytes);
        }
    }

    @Override
    public void disableAutoInboundFlowControl() {
        H2FlowController flowController = getFlowController();
        if (flowController != null) {
            flowController.disableAutoFlowControl(streamId());
        }
    }

    @Override
    public void enableAutoInboundFlowControl() {
        H2FlowController flowController = getFlowController();
        if (flowController != null) {
            flowController.enableAutoFlowControl(streamId());
        }
    }

    @Override
    public boolean isWritable() {
        return http2StreamChannel.isWritable();
    }

    @Override
    public void setOnWritableHandler(Runnable handler) {
        this.onWritableHandler = handler;
        // Register channel listener
        http2StreamChannel.pipeline().addLast(new io.netty.channel.ChannelInboundHandlerAdapter() {
            @Override
            public void channelWritabilityChanged(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
                Runnable currentHandler = onWritableHandler;
                if (currentHandler != null && ctx.channel().isWritable()) {
                    currentHandler.run();
                }
                super.channelWritabilityChanged(ctx);
            }
        });
    }
}
