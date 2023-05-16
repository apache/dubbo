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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public abstract class QueuedCommand {

    protected Channel channel;

    private ChannelPromise promise;

    public ChannelPromise promise() {
        return promise;
    }

    public void promise(ChannelPromise promise) {
        this.promise = promise;
    }

    public void cancel() {
        promise.tryFailure(new IllegalStateException("Canceled"));
    }

    public void run(Channel channel) {
        if (channel.isActive()) {
            channel.write(this).addListener(future -> {
                if (future.isSuccess()) {
                    promise.setSuccess();
                } else {
                    promise.setFailure(future.cause());
                }
            });
        } else {
            promise.trySuccess();
        }
    }

    public final void send(ChannelHandlerContext ctx, ChannelPromise promise) {
        if (ctx.channel().isActive()) {
            doSend(ctx, promise);
        }
    }

    public QueuedCommand channel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public Channel channel() {
        return channel;
    }

    public abstract void doSend(ChannelHandlerContext ctx, ChannelPromise promise);
}

