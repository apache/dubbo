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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.Http2StreamChannel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;

public class NettyH2StreamChannel implements H2StreamChannel {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(NettyH2StreamChannel.class);

    private final Http2StreamChannel http2StreamChannel;

    private final TripleConfig tripleConfig;

    private final Http2Connection http2Connection;

    public NettyH2StreamChannel(
            Http2StreamChannel http2StreamChannel, TripleConfig tripleConfig, Http2Connection http2Connection) {
        this.http2StreamChannel = http2StreamChannel;
        this.tripleConfig = tripleConfig;
        this.http2Connection = http2Connection;
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
    public void consumeBytes(int numBytes) throws Exception {
        if (numBytes <= 0) {
            return;
        }

        if (http2Connection == null) {
            LOGGER.debug("Http2Connection not available, skip consumeBytes");
            return;
        }

        Http2LocalFlowController localFlowController = http2Connection.local().flowController();

        // Get the stream from connection using stream id
        int streamId = http2StreamChannel.stream().id();
        Http2Stream stream = http2Connection.stream(streamId);
        if (stream == null) {
            LOGGER.debug("Stream {} not found in connection, skip consumeBytes", streamId);
            return;
        }

        // Consume bytes to trigger WINDOW_UPDATE frame
        // This must be executed in the event loop thread
        if (http2StreamChannel.eventLoop().inEventLoop()) {
            localFlowController.consumeBytes(stream, numBytes);
        } else {
            http2StreamChannel.eventLoop().execute(() -> {
                try {
                    localFlowController.consumeBytes(stream, numBytes);
                } catch (Exception e) {
                    LOGGER.warn(PROTOCOL_FAILED_RESPONSE, "", "", "Failed to consumeBytes for stream " + streamId, e);
                }
            });
        }
    }

    @Override
    public boolean isReady() {
        return http2StreamChannel.isWritable();
    }
}
