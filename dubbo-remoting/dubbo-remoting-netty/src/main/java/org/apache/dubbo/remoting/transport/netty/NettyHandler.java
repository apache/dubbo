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
package org.apache.dubbo.remoting.transport.netty;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_UNEXPECTED_EXCEPTION;

/**
 * NettyHandler
 */
@Sharable
public class NettyHandler extends SimpleChannelHandler {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NettyHandler.class);

    private final Map<String, Channel> channels = new ConcurrentHashMap<>(); // <ip:port, channel>

    private final URL url;

    private final ChannelHandler handler;

    public NettyHandler(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        org.jboss.netty.channel.Channel ch = ctx.getChannel();
        NettyChannel channel = NettyChannel.getOrAddChannel(ch, url, handler);
        try {
            if (channel != null) {
                channels.put(NetUtils.toAddressString((InetSocketAddress) ch.getRemoteAddress()), channel);
            }
            handler.connected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ch);
        }

        if (logger.isInfoEnabled() && channel != null) {
            logger.info(
                    "The connection {} between {} and {} is established.",
                    ch,
                    channel.getRemoteAddress(),
                    channel.getLocalAddress());
        }
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        org.jboss.netty.channel.Channel ch = ctx.getChannel();
        NettyChannel channel = NettyChannel.getOrAddChannel(ch, url, handler);
        try {
            channels.remove(NetUtils.toAddressString((InetSocketAddress) ch.getRemoteAddress()));
            handler.disconnected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ch);
        }

        if (logger.isInfoEnabled()) {
            logger.info(
                    "The connection {} between {} and {} is disconnected.",
                    ch,
                    channel.getRemoteAddress(),
                    channel.getLocalAddress());
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
        try {
            handler.received(channel, e.getMessage());
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
        }
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        super.writeRequested(ctx, e);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
        try {
            handler.sent(channel, e.getMessage());
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        org.jboss.netty.channel.Channel ch = ctx.getChannel();
        NettyChannel channel = NettyChannel.getOrAddChannel(ch, url, handler);
        try {
            handler.caught(channel, e.getCause());
        } finally {
            NettyChannel.removeChannelIfDisconnected(ch);
        }

        if (logger.isWarnEnabled()) {
            logger.warn(
                    TRANSPORT_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    channel == null
                            ? String.format("The connection %s has exception.", ch)
                            : String.format(
                                    "The connection %s between %s and %s has exception.",
                                    ch, channel.getRemoteAddress(), channel.getLocalAddress()),
                    e.getCause());
        }
    }
}
