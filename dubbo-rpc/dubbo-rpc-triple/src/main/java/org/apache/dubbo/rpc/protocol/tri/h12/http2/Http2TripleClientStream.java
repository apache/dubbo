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
package org.apache.dubbo.rpc.protocol.tri.h12.http2;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.command.CreateStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.stream.AbstractTripleClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.concurrent.Executor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.codec.http2.Http2StreamChannelOption;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;

public final class Http2TripleClientStream extends AbstractTripleClientStream {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(Http2TripleClientStream.class);

    private final Channel parent;

    public Http2TripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            Channel parent,
            ClientStream.Listener listener,
            TripleWriteQueue writeQueue) {
        super(frameworkModel, executor, writeQueue, listener, parent);
        this.parent = parent;
    }

    /**
     * For test only
     */
    public Http2TripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            TripleWriteQueue writeQueue,
            ClientStream.Listener listener,
            Http2StreamChannel http2StreamChannel) {
        super(frameworkModel, executor, writeQueue, listener, http2StreamChannel);
        this.parent = http2StreamChannel.parent();
    }

    @Override
    protected TripleStreamChannelFuture initStreamChannel0(Channel parent) {
        Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(parent);
        // Disable Netty's automatic stream flow control to enable manual flow control
        bootstrap.option(Http2StreamChannelOption.AUTO_STREAM_FLOW_CONTROL, false);
        bootstrap.handler(new ChannelInboundHandlerAdapter() {
            @Override
            public void handlerAdded(ChannelHandlerContext ctx) {
                ctx.channel()
                        .pipeline()
                        .addLast(new TripleCommandOutBoundHandler())
                        .addLast(new TripleHttp2ClientResponseHandler(createTransportListener()));
            }
        });
        TripleStreamChannelFuture streamChannelFuture = new TripleStreamChannelFuture(parent);
        writeQueue.enqueue(CreateStreamQueueCommand.create(bootstrap, streamChannelFuture));
        return streamChannelFuture;
    }

    @Override
    protected void consumeBytes(int numBytes) {
        if (numBytes <= 0) {
            return;
        }

        // todo The current implementation is not optimal, and alternative implementations should be considered.

        Channel streamChannel = getStreamChannelFuture().getNow();
        if (!(streamChannel instanceof Http2StreamChannel)) {
            return;
        }

        Http2StreamChannel http2StreamChannel = (Http2StreamChannel) streamChannel;

        // Get Http2Connection from parent channel pipeline
        Http2Connection http2Connection = getHttp2Connection();
        if (http2Connection == null) {
            LOGGER.debug("Http2Connection not available for flow control");
            return;
        }

        Http2LocalFlowController localFlowController = http2Connection.local().flowController();
        int streamId = http2StreamChannel.stream().id();
        Http2Stream stream = http2Connection.stream(streamId);
        if (stream == null) {
            LOGGER.debug("Stream {} not found in connection, skip consumeBytes", streamId);
            return;
        }

        // Consume bytes to trigger WINDOW_UPDATE frame
        // This must be executed in the event loop thread
        if (http2StreamChannel.eventLoop().inEventLoop()) {
            try {
                localFlowController.consumeBytes(stream, numBytes);
            } catch (Exception e) {
                LOGGER.warn(PROTOCOL_FAILED_RESPONSE, "", "", "Failed to consumeBytes for stream " + streamId, e);
            }
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

    /**
     * Get Http2Connection from parent channel pipeline.
     */
    private Http2Connection getHttp2Connection() {
        if (parent == null) {
            return null;
        }
        ChannelHandlerContext ctx = parent.pipeline().context(Http2FrameCodec.class);
        if (ctx == null) {
            return null;
        }
        Http2FrameCodec codec = (Http2FrameCodec) ctx.handler();
        return codec.connection();
    }
}
