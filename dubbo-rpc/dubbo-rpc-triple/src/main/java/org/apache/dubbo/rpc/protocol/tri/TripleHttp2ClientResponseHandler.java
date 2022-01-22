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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;

import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;

public final class TripleHttp2ClientResponseHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleHttp2ClientResponseHandler.class);

    public TripleHttp2ClientResponseHandler() {
        super(false);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof Http2GoAwayFrame) {
            Http2GoAwayFrame event = (Http2GoAwayFrame) evt;
            ctx.close();
            LOGGER.debug(
                "Event triggered, event name is: " + event.name() + ", last stream id is: " + event.lastStreamId());
        } else if (evt instanceof Http2ResetFrame) {
            onResetRead(ctx, (Http2ResetFrame) evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void onResetRead(ChannelHandlerContext ctx, Http2ResetFrame resetFrame) {
        final AbstractClientStream clientStream = ctx.channel().attr(TripleConstant.CLIENT_STREAM_KEY).get();
        LOGGER.warn("Triple Client received remote reset errorCode=" + resetFrame.errorCode());
        clientStream.cancelByRemote();
        ctx.close();
    }

    private void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame msg) {
        Http2Headers headers = msg.headers();
        final AbstractClientStream clientStream = ctx.channel().attr(TripleConstant.CLIENT_STREAM_KEY).get();
        CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
        if (null != messageEncoding) {
            String compressorStr = messageEncoding.toString();
            if (!DEFAULT_COMPRESSOR.equals(compressorStr)) {
                Compressor compressor = Compressor.getCompressor(clientStream.getUrl().getOrDefaultFrameworkModel(), compressorStr);
                if (null == compressor) {
                    throw GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                        .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr))
                        .asException();
                } else {
                    clientStream.setDeCompressor(compressor);
                }
            }
        }
        final TransportObserver observer = clientStream.inboundTransportObserver();
        observer.onMetadata(new Http2HeaderMeta(headers), false);
        if (msg.isEndStream()) {
            observer.onComplete();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final AbstractClientStream clientStream = ctx.channel().attr(TripleConstant.CLIENT_STREAM_KEY).get();
        final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withCause(cause);
        Metadata metadata = new DefaultMetadata();
        metadata.put(TripleHeaderEnum.STATUS_KEY.getHeader(), Integer.toString(status.code.code));
        metadata.put(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.toMessage());
        LOGGER.warn("Meet Exception on ClientResponseHandler, status code is: " + status.code, cause);
        clientStream.inboundMessageObserver().onError(status.asException());
        ctx.close();
    }

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        super.channelRead(ctx, msg.content());
        if (msg.isEndStream()) {
            final AbstractClientStream clientStream = ctx.channel().attr(TripleConstant.CLIENT_STREAM_KEY).get();
            // stream already closed;
            if (clientStream != null) {
                clientStream.inboundTransportObserver().onComplete();
            }
        }
    }
}
