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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import org.apache.dubbo.common.BatchExecutorQueue;
import org.apache.dubbo.rpc.protocol.tri.command.FrameQueueCommand;

public class TripleWriteQueue extends BatchExecutorQueue<FrameQueueCommand> {

    public ChannelFuture enqueue(FrameQueueCommand command) {
        return enqueueSoon(command, true);
    }

    public ChannelFuture enqueueSoon(FrameQueueCommand command, boolean soon) {
        ChannelPromise promise = command.promise();
        if (promise == null) {
            Channel ch = command.channel();
            promise = ch.newPromise();
            command.promise(promise);
        }
        super.enqueueSoon(command, soon, command.channel().eventLoop());
        return promise;
    }

    @Override
    protected void prepare(FrameQueueCommand item) {
        item.run(item.channel());
    }

    @Override
    protected void flush(FrameQueueCommand item) {
        Channel channel = item.channel();
        item.run(channel);
        channel.flush();
    }
}
