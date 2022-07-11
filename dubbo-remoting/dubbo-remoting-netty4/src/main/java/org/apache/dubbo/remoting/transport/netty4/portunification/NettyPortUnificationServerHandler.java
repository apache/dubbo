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
package org.apache.dubbo.remoting.transport.netty4.portunification;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@io.netty.channel.ChannelHandler.Sharable
public class NettyPortUnificationServerHandler extends ChannelDuplexHandler {
    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>();

    private static final Logger logger = LoggerFactory.getLogger(NettyPortUnificationServerHandler.class);

    public Map<String, Channel> getChannels() {
        return channels;
    }


    public NettyPortUnificationServerHandler(ChannelHandler handler) {
        this.handler = handler;
    }

    // this handler will be bind to channelWithHandler and will be updated after recognizing
    private final ChannelHandler handler;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        PuNettyChannel channel = PuNettyChannel.getOrAddChannel(ctx.channel(), handler);
        if (channel == null) {
            return;
        }else {
            channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
        }

        handler.connected(channel);

        if (logger.isInfoEnabled()) {
            logger.info("The connection of " + channel.getRemoteAddress() + " -> " + channel.getLocalAddress() + " is established.");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        PuNettyChannel channel = PuNettyChannel.getOrAddChannel(ctx.channel(), handler);
        try {
            channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
            handler.disconnected(channel);
        } finally {
            PuNettyChannel.removeChannel(ctx.channel());
        }

        if (logger.isInfoEnabled()) {
            logger.info("The connection of " + channel.getRemoteAddress() + " -> " + channel.getLocalAddress() + " is disconnected.");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PuNettyChannel channel = PuNettyChannel.getOrAddChannel(ctx.channel(), handler);
        handler.received(channel, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        PuNettyChannel channel = PuNettyChannel.getOrAddChannel(ctx.channel(), handler);
        handler.sent(channel, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // server will close channel when server don't receive any heartbeat from client util timeout.
        if (evt instanceof IdleStateEvent) {
            PuNettyChannel channel = PuNettyChannel.getOrAddChannel(ctx.channel(), handler);
            try {
                logger.info("IdleStateEvent triggered, close channel " + channel);
                channel.close();
            } finally {
                PuNettyChannel.removeChannelIfDisconnected(ctx.channel());
            }
        }
        super.userEventTriggered(ctx, evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        throws Exception {
        PuNettyChannel channel = PuNettyChannel.getOrAddChannel(ctx.channel(), handler);
        try {
            handler.caught(channel, cause);
        } finally {
            PuNettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }
}
