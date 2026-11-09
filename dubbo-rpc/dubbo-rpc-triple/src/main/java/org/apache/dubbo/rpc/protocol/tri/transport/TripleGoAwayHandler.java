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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.connection.ConnectionHandler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.util.ReferenceCountUtil;

/**
 * Handles HTTP/2 GOAWAY frames on the Triple protocol client side.
 *
 * <p>Logs the GOAWAY errorCode and lastStreamId for diagnostics, then delegates
 * to {@link ConnectionHandler#onGoAway} which performs graceful connection migration
 * (keeping old channel alive until new connection is established).
 */
public class TripleGoAwayHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(TripleGoAwayHandler.class);

    public TripleGoAwayHandler() {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2GoAwayFrame) {
            Http2GoAwayFrame goAwayFrame = (Http2GoAwayFrame) msg;
            final ConnectionHandler connectionHandler =
                    (ConnectionHandler) ctx.pipeline().get(Constants.CONNECTION_HANDLER_NAME);

            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                        "Received GOAWAY frame: %s -> %s, errorCode=%d, lastStreamId=%d. "
                                + "Initiating graceful connection migration.",
                        ctx.channel().localAddress(),
                        ctx.channel().remoteAddress(),
                        goAwayFrame.errorCode(),
                        goAwayFrame.lastStreamId()));
            }

            connectionHandler.onGoAway(ctx.channel());
            ReferenceCountUtil.release(msg);
            return;
        }
        super.channelRead(ctx, msg);
    }
}
