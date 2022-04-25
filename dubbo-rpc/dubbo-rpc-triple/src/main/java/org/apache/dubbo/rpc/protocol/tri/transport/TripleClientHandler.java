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

package org.apache.dubbo.rpc.protocol.tri.transport;

import org.apache.dubbo.remoting.api.ConnectionHandler;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TripleClientHandler extends ChannelDuplexHandler {

    static final long CLIENT_SCHEDULE_PING = 0x1141a85a98L;

    private final FrameworkModel frameworkModel;

    private Http2FrameCodec codec;

    private ScheduledExecutorService pingExecutor;

    private int heartbeatInterval;

    private int lossConnectCount = 0;

    public TripleClientHandler(FrameworkModel frameworkModel, Http2FrameCodec codec,
                               int heartbeatInterval) {
        this.frameworkModel = frameworkModel;
        this.codec = codec;
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        lossConnectCount = 0;
        if (msg instanceof Http2GoAwayFrame) {
            final ConnectionHandler connectionHandler = ctx.pipeline().get(ConnectionHandler.class);
            connectionHandler.onGoAway(ctx.channel());
        }
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        pingExecutor.schedule(() -> {
            if (codec.connection().numActiveStreams() <= 0 && lossConnectCount > 2) {
                ctx.channel().close();
                pingExecutor.shutdown();
                return;
            }
            if (codec.connection().numActiveStreams() <= 0) {
                lossConnectCount++;
                Http2PingFrame pingFrame = new DefaultHttp2PingFrame(CLIENT_SCHEDULE_PING, true);
                ctx.write(pingFrame);
            }
        }, heartbeatInterval, TimeUnit.SECONDS);
    }
}
