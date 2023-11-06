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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

import java.net.InetSocketAddress;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(NettyChannelHandler.class);

    private final Map<String, Channel> dubboChannels;

    private final URL url;
    private final ChannelHandler handler;

    public NettyChannelHandler(Map<String, Channel> dubboChannels, URL url, ChannelHandler handler) {
        this.dubboChannels = dubboChannels;
        this.url = url;
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        if (channel != null) {
            dubboChannels.put(
                    NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
            handler.connected(channel);

            if (logger.isInfoEnabled()) {
                logger.info("The connection of " + channel.getRemoteAddress() + " -> " + channel.getLocalAddress()
                        + " is established.");
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            dubboChannels.remove(
                    NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
            if (channel != null) {
                handler.disconnected(channel);
                if (logger.isInfoEnabled()) {
                    logger.info("The connection of " + channel.getRemoteAddress() + " -> " + channel.getLocalAddress()
                            + " is disconnected.");
                }
            }
        } finally {
            NettyChannel.removeChannel(ctx.channel());
        }
    }
}
