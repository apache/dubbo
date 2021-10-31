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

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;

public class GracefulShutdown {
    static final long GRACEFUL_SHUTDOWN_PING = 0x97ACEF001L;
    private static final long GRACEFUL_SHUTDOWN_PING_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10);
    private final ChannelHandlerContext ctx;
    private final ChannelPromise originPromise;
    private final String goAwayMessage;
    private boolean pingAckedOrTimeout;
    private Future<?> pingFuture;

    public GracefulShutdown(ChannelHandlerContext ctx, String goAwayMessage, ChannelPromise originPromise) {
        this.ctx = ctx;
        this.goAwayMessage = goAwayMessage;
        this.originPromise = originPromise;
    }

    public void gracefulShutdown() {
        Http2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(Http2Error.NO_ERROR, ByteBufUtil
            .writeAscii(ctx.alloc(), goAwayMessage));
        goAwayFrame.setExtraStreamIds(Integer.MAX_VALUE);
        ctx.write(goAwayFrame);
        pingFuture = ctx.executor().schedule(
            () -> secondGoAwayAndClose(ctx),
            GRACEFUL_SHUTDOWN_PING_TIMEOUT_NANOS,
            TimeUnit.NANOSECONDS);

        Http2PingFrame pingFrame = new DefaultHttp2PingFrame(GRACEFUL_SHUTDOWN_PING, false);
        ctx.write(pingFrame);
    }

    void secondGoAwayAndClose(ChannelHandlerContext ctx) {
        if (pingAckedOrTimeout) {
            return;
        }
        pingAckedOrTimeout = true;

        pingFuture.cancel(false);

        try {
            Http2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(Http2Error.NO_ERROR,
                ByteBufUtil.writeAscii(this.ctx.alloc(), this.goAwayMessage));
            ctx.write(goAwayFrame);
            ctx.flush();
            //TODO support customize graceful shutdown timeout mills
            ctx.close(originPromise);
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }
}
