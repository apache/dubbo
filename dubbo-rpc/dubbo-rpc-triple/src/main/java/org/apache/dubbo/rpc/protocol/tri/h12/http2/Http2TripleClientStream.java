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
package org.apache.dubbo.rpc.protocol.tri.h12.http2;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.command.CreateStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.stream.AbstractTripleClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.concurrent.Executor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;

public final class Http2TripleClientStream extends AbstractTripleClientStream {

    public Http2TripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            Channel parent,
            ClientStream.Listener listener,
            TripleWriteQueue writeQueue) {
        super(frameworkModel, executor, writeQueue, listener, parent);
    }

    /**
     * For test only
     */
    public Http2TripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            TripleWriteQueue writeQueue,
            ClientStream.Listener listener,
            Http2StreamChannel http2StreamChannel) {
        super(frameworkModel, executor, writeQueue, listener, http2StreamChannel);
    }

    @Override
    protected TripleStreamChannelFuture initStreamChannel(Channel parent) {
        Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(parent);
        bootstrap.handler(new ChannelInboundHandlerAdapter() {
            @Override
            public void handlerAdded(ChannelHandlerContext ctx) {
                ctx.channel()
                        .pipeline()
                        .addLast(new TripleCommandOutBoundHandler())
                        .addLast(new TripleHttp2ClientResponseHandler(createTransportListener()));
            }
        });
        TripleStreamChannelFuture streamChannelFuture = new TripleStreamChannelFuture(parent);
        writeQueue.enqueue(CreateStreamQueueCommand.create(bootstrap, streamChannelFuture));
        return streamChannelFuture;
    }
}
