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
package org.apache.dubbo.rpc.protocol.tri.command;

import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class Http3CreateStreamQueueCommand extends QueuedCommand {

    private final ChannelInitializer<QuicStreamChannel> initializer;

    private final TripleStreamChannelFuture streamChannelFuture;

    private Http3CreateStreamQueueCommand(
            ChannelInitializer<QuicStreamChannel> initializer, TripleStreamChannelFuture future) {
        this.initializer = initializer;
        this.streamChannelFuture = future;
        this.promise(future.getParentChannel().newPromise());
        this.channel(future.getParentChannel());
    }

    public static Http3CreateStreamQueueCommand create(
            ChannelInitializer<QuicStreamChannel> initializer, TripleStreamChannelFuture future) {
        return new Http3CreateStreamQueueCommand(initializer, future);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {}

    @Override
    public void run(Channel channel) {
        Http3.newRequestStream((QuicChannel) channel, initializer).addListener(future -> {
            if (future.isSuccess()) {
                streamChannelFuture.complete((Channel) future.getNow());
            } else {
                streamChannelFuture.completeExceptionally(future.cause());
            }
        });
    }
}
