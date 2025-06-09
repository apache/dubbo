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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.connection.ConnectionHandler;

import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_UNEXPECTED_EXCEPTION;

@Sharable
public class NettyConnectionHandler extends ChannelInboundHandlerAdapter implements ConnectionHandler {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(NettyConnectionHandler.class);

    private static final AttributeKey<Boolean> GO_AWAY_KEY = AttributeKey.valueOf("dubbo_channel_goaway");
    private final AbstractNettyConnectionClient connectionClient;

    public NettyConnectionHandler(AbstractNettyConnectionClient connectionClient) {
        this.connectionClient = connectionClient;
    }

    @Override
    public void onGoAway(Object channel) {
        if (!(channel instanceof Channel)) {
            return;
        }
        Channel nettyChannel = ((Channel) channel);
        Attribute<Boolean> attr = nettyChannel.attr(GO_AWAY_KEY);
        if (Boolean.TRUE.equals(attr.get())) {
            return;
        }

        attr.set(true);
        if (connectionClient != null) {
            connectionClient.onGoaway(nettyChannel);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Channel {} go away ,schedule reconnect", nettyChannel);
        }
        reconnect(nettyChannel);
    }

    @Override
    public void reconnect(Object channel) {
        if (!(channel instanceof Channel)) {
            return;
        }
        Channel nettyChannel = ((Channel) channel);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Connection:{} is reconnecting, attempt={}", connectionClient, 1);
        }
        EventLoop eventLoop = nettyChannel.eventLoop();
        if (connectionClient.isClosed()) {
            LOGGER.info("The connection {} has been closed and will not reconnect", connectionClient);
            return;
        }
        eventLoop.schedule(connectionClient::doReconnect, 1, TimeUnit.SECONDS);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
        Channel ch = ctx.channel();
        NettyChannel channel = NettyChannel.getOrAddChannel(ch, connectionClient.getUrl(), connectionClient);
        if (!connectionClient.isClosed()) {
            connectionClient.onConnected(ch);

            if (LOGGER.isInfoEnabled() && channel != null) {
                LOGGER.info(
                        "The connection {} of {} -> {} is established.",
                        ch,
                        channel.getLocalAddressKey(),
                        channel.getRemoteAddressKey());
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel ch = ctx.channel();
        NettyChannel channel = NettyChannel.getOrAddChannel(ch, connectionClient.getUrl(), connectionClient);
        try {
            Attribute<Boolean> goawayAttr = ch.attr(GO_AWAY_KEY);
            if (!Boolean.TRUE.equals(goawayAttr.get())) {
                reconnect(ch);
            }

            if (LOGGER.isInfoEnabled() && channel != null) {
                LOGGER.info(
                        "The connection {} of {} -> {} is disconnected.",
                        ch,
                        channel.getLocalAddressKey(),
                        channel.getRemoteAddressKey());
            }
        } finally {
            NettyChannel.removeChannel(ch);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn(TRANSPORT_UNEXPECTED_EXCEPTION, "", "", String.format("Channel error:%s", ctx.channel()), cause);
        ctx.close();
    }
}
