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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2SettingsFrame;

public class TripleHttp2SettingsHandler extends SimpleChannelInboundHandler<Http2SettingsFrame> {

    private static final Logger logger = LoggerFactory.getLogger(TripleHttp2SettingsHandler.class);

    public TripleHttp2SettingsHandler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2SettingsFrame msg) throws Exception {
        final ConnectionHandler connectionHandler =
                (ConnectionHandler) ctx.pipeline().get(Constants.CONNECTION_HANDLER_NAME);
        if (logger.isDebugEnabled()) {
            logger.debug("Receive Http2 Settings frame of " + ctx.channel().localAddress() + " -> "
                    + ctx.channel().remoteAddress());
        }
        // Notify the connection preface is received.
        connectionHandler.onConnectionPrefaceReceived(ctx.channel());
    }
}
