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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * handle the IdleStateEvent triggered by IdleStateHandler
 */
public class HeartbeatServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private int lossConnectCount = 0;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                lossConnectCount++;
                if (lossConnectCount > 2) {
                    log.warn(String.format("Server read idle, close:%s", ctx.channel()));
                    ctx.close();
                }
            } else if (e.state() == IdleState.WRITER_IDLE) {
                log.warn(String.format("Server write idle, channel:%s", ctx.channel()));
            } else if (e.state() == IdleState.ALL_IDLE) {
                log.warn("Server All idle");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        lossConnectCount = 0;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info(String.format("Channel:%s ", ctx.channel() + "is active"));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info(String.format("Channel:%s ", ctx.channel() + "is inactive"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(String.format("Channel error:%s ", ctx.channel()), cause);
        ctx.close();
    }

}
