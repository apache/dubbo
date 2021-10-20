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
        if (msg instanceof Request) {
            writeRequest(ctx, (Request) msg, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private void writeRequest(ChannelHandlerContext ctx, final Request req, final ChannelPromise promise) {
        DefaultFuture2.addTimeoutListener(req.getId(), ctx::close);
        Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        final AbstractClientStream stream = AbstractClientStream.newClientStream(req, connection);
        final ClientTransportObserver clientTransportObserver = new ClientTransportObserver(ctx, promise);
        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(ctx.channel());
        streamChannelBootstrap.open()
            .addListener(future -> {
                if (future.isSuccess()) {
                    final Http2StreamChannel curChannel = (Http2StreamChannel) future.get();
                    curChannel.pipeline()
                        .addLast(new TripleHttp2ClientResponseHandler())
                        .addLast(new GrpcDataDecoder(Integer.MAX_VALUE, true))
                        .addLast(new TripleClientInboundHandler());
                    curChannel.attr(TripleConstant.CLIENT_STREAM_KEY).set(stream);
                    clientTransportObserver.setStreamChannel(curChannel);
                } else {
                    clientTransportObserver.initializedFailed();
                    promise.tryFailure(future.cause());
                    DefaultFuture2.getFuture(req.getId()).cancel();
                }
            });

        stream
            .subscribe(clientTransportObserver);
        // start call
        stream.startCall();
    }
}
