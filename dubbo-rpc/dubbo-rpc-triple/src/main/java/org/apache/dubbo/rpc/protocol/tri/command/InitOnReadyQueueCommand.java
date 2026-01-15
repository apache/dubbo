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

import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Command to trigger initial onReady after the stream channel is created.
 * This is necessary because onReady is only triggered by channelWritabilityChanged,
 * which won't fire if the channel is always writable from creation.
 *
 * <p>This command should be enqueued immediately after CreateStreamQueueCommand.
 * Since the WriteQueue executes commands in order within the EventLoop,
 * this command will run after the stream channel has been created.
 */
public class InitOnReadyQueueCommand extends QueuedCommand {

    private final TripleStreamChannelFuture streamChannelFuture;

    private final ClientStream.Listener listener;

    private InitOnReadyQueueCommand(TripleStreamChannelFuture streamChannelFuture, ClientStream.Listener listener) {
        this.streamChannelFuture = streamChannelFuture;
        this.listener = listener;
        this.promise(streamChannelFuture.getParentChannel().newPromise());
        this.channel(streamChannelFuture.getParentChannel());
    }

    public static InitOnReadyQueueCommand create(
            TripleStreamChannelFuture streamChannelFuture, ClientStream.Listener listener) {
        return new InitOnReadyQueueCommand(streamChannelFuture, listener);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        // NOOP - this command does not send any data
    }

    @Override
    public void run(Channel channel) {
        // Work in I/O thread, after CreateStreamQueueCommand has completed
        Channel streamChannel = streamChannelFuture.getNow();
        if (streamChannel != null && streamChannel.isWritable()) {
            // Trigger initial onReady to allow application to start sending.
            listener.onReady();
        }
    }
}
