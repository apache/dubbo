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

package org.apache.dubbo.rpc.protocol.tri.transport;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.TriRpcStatus;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_SERIALIZE_TRIPLE;

public final class TripleHttp2ClientResponseHandler extends
    SimpleChannelInboundHandler<Http2StreamFrame> {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(
        TripleHttp2ClientResponseHandler.class);
    private final H2TransportListener transportListener;

    public TripleHttp2ClientResponseHandler(H2TransportListener listener) {
        super(false);
        this.transportListener = listener;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof Http2GoAwayFrame) {
            Http2GoAwayFrame event = (Http2GoAwayFrame) evt;
            ctx.close();
            LOGGER.debug(
                "Event triggered, event name is: " + event.name() + ", last stream id is: "
                    + event.lastStreamId());
        } else if (evt instanceof Http2ResetFrame) {
            onResetRead(ctx, (Http2ResetFrame) evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            final Http2HeadersFrame headers = (Http2HeadersFrame) msg;
            transportListener.onHeader(headers.headers(), headers.isEndStream());
        } else if (msg instanceof Http2DataFrame) {
            final Http2DataFrame data = (Http2DataFrame) msg;
            transportListener.onData(data.content(), data.isEndStream());
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void onResetRead(ChannelHandlerContext ctx, Http2ResetFrame resetFrame) {
        LOGGER.warn(PROTOCOL_FAILED_SERIALIZE_TRIPLE, "", "", "Triple Client received remote reset errorCode=" + resetFrame.errorCode());
        transportListener.cancelByRemote(resetFrame.errorCode());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final TriRpcStatus status = TriRpcStatus.INTERNAL
            .withCause(cause);
        LOGGER.warn(PROTOCOL_FAILED_SERIALIZE_TRIPLE, "", "", "Meet Exception on ClientResponseHandler, status code is: " + status.code,
            cause);
        transportListener.cancelByRemote(Http2Error.INTERNAL_ERROR.code());
        ctx.close();
    }

}
