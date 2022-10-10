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

package org.apache.dubbo.remoting.transport.netty4;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class WriteQueue {

    static final int DEQUE_CHUNK_SIZE = 128;
    private final Channel channel;
    private final Queue<MessageTuple> queue;
    private final AtomicBoolean scheduled;

    public WriteQueue(Channel channel) {
        this.channel = channel;
        queue = new ConcurrentLinkedQueue<>();
        scheduled = new AtomicBoolean(false);
    }

    public ChannelFuture enqueue(Object message) {
        MessageTuple msg = new MessageTuple(message, this.channel.newPromise());
        queue.add(msg);
        scheduleFlush();
        return msg.getChannelPromise();
    }

    public void scheduleFlush() {
        if (scheduled.compareAndSet(false, true)) {
            this.channel.eventLoop().execute(this::flush);
        }
    }

    private void flush() {
        try {
            MessageTuple message;
            int i = 0;
            boolean flushedOnce = false;
            while ((message = queue.poll()) != null) {
                this.channel.write(message.getMessage(), message.getChannelPromise());
                i++;
                if (i == DEQUE_CHUNK_SIZE) {
                    i = 0;
                    this.channel.flush();
                    flushedOnce = true;
                }
            }
            if (i != 0 || !flushedOnce) {
                this.channel.flush();
            }
        } finally {
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush();
            }
        }
    }


    private static class MessageTuple {

        private final Object message;

        private final ChannelPromise channelPromise;

        public MessageTuple(Object message, ChannelPromise channelPromise) {
            this.message = message;
            this.channelPromise = channelPromise;
        }

        public Object getMessage() {
            return message;
        }

        public ChannelPromise getChannelPromise() {
            return channelPromise;
        }
    }
}
