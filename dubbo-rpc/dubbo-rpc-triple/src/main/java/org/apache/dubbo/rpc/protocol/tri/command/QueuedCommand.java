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

public interface QueuedCommand {

    void setFlush(boolean flush);

    ChannelPromise promise();

    void promise(ChannelPromise promise);

    void run(Channel channel);

    abstract class AbstractQueuedCommand implements QueuedCommand {

        private ChannelPromise promise;

        protected boolean flush = false;

        @Override
        public ChannelPromise promise() {
            return promise;
        }

        public void setFlush(boolean flush) {
            this.flush = flush;
        }

        @Override
        public void promise(ChannelPromise promise) {
            this.promise = promise;
        }

        @Override
        public void run(Channel channel) {
            channel.write(this, promise);
        }

        public final void send(ChannelHandlerContext ctx, ChannelPromise promise) {
            doSend(ctx, promise);
            if (flush) {
                ctx.flush();
            }
        }

        public abstract void doSend(ChannelHandlerContext ctx, ChannelPromise promise);
    }

}
