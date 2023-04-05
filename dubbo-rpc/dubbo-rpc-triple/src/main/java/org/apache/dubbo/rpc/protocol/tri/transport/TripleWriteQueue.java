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

import org.apache.dubbo.common.BatchExecutorQueue;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2StreamChannel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class TripleWriteQueue extends BatchExecutorQueue<QueuedCommand> {

    private volatile TripleStreamChannelFuture channelFuture = new TripleStreamChannelFuture();

    public TripleWriteQueue() {
    }

    public TripleWriteQueue(Http2StreamChannel channel) {
        Assert.notNull(channel, "Stream channel not null");
        channelFuture.complete(channel);
    }

    public TripleWriteQueue(Http2StreamChannel channel, int chunkSize) {
        super(chunkSize);
        Assert.notNull(channel, "Stream channel not null");
        channelFuture.complete(channel);
    }

    public ChannelFuture enqueue(QueuedCommand command) {
        return this.enqueueFuture(command, channel().eventLoop());
    }

    public ChannelFuture enqueueFuture(QueuedCommand command, Executor executor) {
        ChannelPromise promise = command.promise();
        if (promise == null) {
            promise = channel().newPromise();
            command.promise(promise);
        }
        super.enqueue(command, executor);
        return promise;
    }

    @Override
    protected void prepare(QueuedCommand item) {
        Throwable throwable = channelFuture.cause();
        if (throwable != null) {
            item.promise().setFailure(throwable);
        } else {
            item.run(channel());
        }
    }

    @Override
    protected void flush(QueuedCommand item) {
        Throwable cause = channelFuture.cause();
        if (cause != null) {
            item.promise().setFailure(cause);
        } else {
            Http2StreamChannel channel = channel();
            item.run(channel);
            channel.parent().flush();
        }
    }

    public Http2StreamChannel channel() {
        if (channelFuture.cause() != null) {
            throw new RuntimeException(channelFuture.cause());
        }

        try {
            return channelFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetChannelFuture(Http2StreamChannel channel) {
        resetChannelFuture();
        channelFuture.complete(channel);
    }

    public void resetChannelFuture(Throwable cause) {
        resetChannelFuture();
        channelFuture.completeExceptionally(cause);
    }

    private void resetChannelFuture() {
        if (channelFuture.isDone()
            || channelFuture.isCompletedExceptionally()
            || channelFuture.isCancelled()) {
            channelFuture = new TripleStreamChannelFuture();
        }
    }
}
