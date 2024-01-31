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
package org.apache.dubbo.rpc.protocol.tri;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.handler.timeout.IdleStateEvent;

public class TriplePingPongHandler extends ChannelDuplexHandler {

    private final long pingAckTimeout;

    private ScheduledFuture<?> pingAckTimeoutFuture;

    public TriplePingPongHandler(long pingAckTimeout) {
        this.pingAckTimeout = pingAckTimeout;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Http2PingFrame) || pingAckTimeoutFuture == null) {
            super.channelRead(ctx, msg);
            return;
        }
        // cancel task when read anything, include http2 ping ack
        pingAckTimeoutFuture.cancel(true);
        pingAckTimeoutFuture = null;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            ctx.fireUserEventTriggered(evt);
            return;
        }
        ctx.writeAndFlush(new DefaultHttp2PingFrame(0));
        if (pingAckTimeoutFuture == null) {
            pingAckTimeoutFuture =
                    ctx.executor().schedule(new CloseChannelTask(ctx), pingAckTimeout, TimeUnit.MILLISECONDS);
        }
        // not null means last ping ack not received
    }

    private static class CloseChannelTask implements Runnable {

        private final ChannelHandlerContext ctx;

        public CloseChannelTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (ctx.channel().isActive()) {
                ctx.close();
            }
        }
    }
}
