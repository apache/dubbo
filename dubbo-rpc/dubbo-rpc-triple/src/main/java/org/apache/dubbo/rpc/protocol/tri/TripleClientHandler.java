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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.ConnectionHandler;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.RpcInvocation;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;

public class TripleClientHandler extends ChannelDuplexHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Request) {
            writeRequest(ctx, (Request) msg, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2SettingsFrame) {
            // already handled
        } else if (msg instanceof Http2GoAwayFrame) {
            final ConnectionHandler connectionHandler = ctx.pipeline().get(ConnectionHandler.class);
            connectionHandler.onGoAway(ctx.channel());
            ReferenceCountUtil.release(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private void writeRequest(ChannelHandlerContext ctx, final Request req, ChannelPromise promise) throws IOException {
        final RpcInvocation inv = (RpcInvocation) req.getData();
        final boolean needWrapper = TripleUtil.needWrapper(inv.getParameterTypes());
        final URL url = inv.getInvoker().getUrl();
        ClientStream clientStream = new ClientStream(url, ctx, needWrapper, req);
        clientStream.write(req, promise);
    }
}
