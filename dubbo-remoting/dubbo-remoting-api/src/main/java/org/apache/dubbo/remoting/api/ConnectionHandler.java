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
package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class ConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);
    private static final AttributeKey<Boolean> GO_AWAY_KEY = AttributeKey.valueOf("dubbo_channel_goaway");
    private final Connection connection;

    public ConnectionHandler(Connection connection) {
        this.connection = connection;
    }

    public void onGoAway(Channel channel) {
        final Attribute<Boolean> attr = channel.attr(GO_AWAY_KEY);
        if (Boolean.TRUE.equals(attr.get())) {
            return;
        }

        attr.set(true);
        if (connection != null) {
            connection.onGoaway(channel);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Channel %s go away ,schedule reconnect", channel));
        }
        reconnect(channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
        if (!connection.isClosed()) {
            connection.onConnected(ctx.channel());
        } else {
            ctx.close();
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn(String.format("Channel error:%s", ctx.channel()), cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        final Attribute<Boolean> goawayAttr = ctx.channel().attr(GO_AWAY_KEY);
        if (!Boolean.TRUE.equals(goawayAttr.get())) {
            reconnect(ctx.channel());
        }
    }

    private void reconnect(Channel channel) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Connection %s is reconnecting, attempt=%d", connection, 1));
        }
        final EventLoop eventLoop = channel.eventLoop();
        eventLoop.schedule(connection::connect, 1, TimeUnit.SECONDS);
    }

}
