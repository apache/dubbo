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
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

public abstract class StreamQueueCommand extends QueuedCommand {

    protected final TripleStreamChannelFuture streamChannelFuture;

    protected StreamQueueCommand(TripleStreamChannelFuture streamChannelFuture) {
        Assert.notNull(streamChannelFuture, "streamChannelFuture cannot be null.");
        this.streamChannelFuture = streamChannelFuture;
        this.promise(streamChannelFuture.getParentChannel().newPromise());
    }

    @Override
    public void run(Channel channel) {
        if (streamChannelFuture.isSuccess()) {
            super.run(channel);
            return;
        }
        promise().setFailure(streamChannelFuture.cause());
    }

    @Override
    public Channel channel() {
        return this.streamChannelFuture.getNow();
    }
}
