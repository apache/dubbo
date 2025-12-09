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
package org.apache.dubbo.remoting.http12.netty4.h2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2SettingsFrame;

/**
 * Add NettyHttp2SettingsHandler to pipeline because NettyHttp2FrameCodec could not receive Http2SettingsFrame.
 * Http2SettingsFrame does not belong to Http2StreamFrame or Http2GoAwayFrame that Http2MultiplexHandler
 * could process, NettyHttp2FrameCodec is wrapped in Http2MultiplexHandler as a child handler.
 */
public class NettyHttp2SettingsHandler extends SimpleChannelInboundHandler<Http2SettingsFrame> {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttp2SettingsHandler.class);

    /**
     * Http2SettingsFrame arrival notification subscribers.
     */
    private final Set<NettyHttp2FrameCodec> settingsFrameArrivalSubscribers = new HashSet<>();

    private boolean settingsFrameArrived;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2SettingsFrame msg) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Receive client Http2 Settings frame of "
                    + ctx.channel().localAddress() + " <- " + ctx.channel().remoteAddress());
        }
        settingsFrameArrived = true;

        // Notify all subscribers that Http2SettingsFrame is arrived.
        for (NettyHttp2FrameCodec nettyHttp2FrameCodec : settingsFrameArrivalSubscribers) {
            nettyHttp2FrameCodec.notifySettingsFrameArrival();
        }
        settingsFrameArrivalSubscribers.clear();

        ctx.pipeline().remove(this);
    }

    /**
     * Save Http2SettingsFrame arrival notification subscriber if Http2SettingsFrame is not arrived.
     * @param nettyHttp2FrameCodec the netty HTTP2 frame codec that will be notified.
     * @return true: subscribe successful, false: Http2SettingsFrame arrived.
     */
    public boolean subscribeSettingsFrameArrival(NettyHttp2FrameCodec nettyHttp2FrameCodec) {
        if (!settingsFrameArrived) {
            settingsFrameArrivalSubscribers.add(nettyHttp2FrameCodec);
            return true;
        }
        return false;
    }

    public void unsubscribeSettingsFrameArrival(NettyHttp2FrameCodec nettyHttp2FrameCodec) {
        settingsFrameArrivalSubscribers.remove(nettyHttp2FrameCodec);
    }
}
