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

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TripleWritePingHandler extends ChannelDuplexHandler {

    private final long writePingIntervalInMs;

    private ScheduledFuture<?> scheduledFuture;

    public TripleWritePingHandler(long writePingIntervalInMs) {
        this.writePingIntervalInMs = writePingIntervalInMs;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        scheduledFuture = ctx.executor().scheduleWithFixedDelay(new WritePingTask(ctx.channel()), writePingIntervalInMs, writePingIntervalInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (scheduledFuture == null) {
            return;
        }
        scheduledFuture.cancel(true);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)
                || !IdleState.READER_IDLE.equals(((IdleStateEvent) evt).state())) {
            ctx.fireUserEventTriggered(evt);
            return;
        }
        //idle timeout(3 * heartbeat)
        ctx.close();
    }

    private static class WritePingTask implements Runnable {

        private final Channel channel;

        public WritePingTask(Channel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            channel.writeAndFlush(new DefaultHttp2PingFrame(0));
        }
    }

}
