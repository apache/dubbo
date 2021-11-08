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

import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;

public class TripleClientRequestHandler extends ChannelDuplexHandler {

    private final FrameworkModel frameworkModel;

    public TripleClientRequestHandler(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof Request)) {
            super.write(ctx, msg, promise);
            return;
        }
        final Request req = (Request) msg;
        Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        final AbstractClientStream stream = AbstractClientStream.newClientStream(req, connection);
        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(ctx.channel());
        streamChannelBootstrap.open()
            .addListener(future -> {
                if (future.isSuccess()) {
                    final Http2StreamChannel channel = (Http2StreamChannel) future.get();
                    channel.pipeline()
                        .addLast(new TripleCommandOutBoundHandler())
                        .addLast(new TripleHttp2ClientResponseHandler())
                        .addLast(new GrpcDataDecoder(Integer.MAX_VALUE, true))
                        .addLast(new TripleClientInboundHandler());
                    channel.attr(TripleConstant.CLIENT_STREAM_KEY).set(stream);
                    DefaultFuture2.addTimeoutListener(req.getId(), channel::close);
                    WriteQueue writeQueue = new WriteQueue(channel);
                    // Start call only when the channel creation is successful
                    stream.startCall(writeQueue, promise);
                } else {
                    promise.tryFailure(future.cause());
                    DefaultFuture2.getFuture(req.getId()).cancel();
                }
            });
    }
}
