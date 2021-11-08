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

import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class WriteQueue {

    static final int DEQUE_CHUNK_SIZE = 128;
    private final Channel channel;
    private final Queue<QueuedCommand> queue;
    private final AtomicBoolean scheduled;

    public WriteQueue(Channel channel) {
        this.channel = channel;
        queue = new ConcurrentLinkedQueue<>();
        scheduled = new AtomicBoolean(false);
    }

    public ChannelPromise enqueue(QueuedCommand command, boolean flush) {
        ChannelPromise promise = command.promise();
        if (promise == null) {
            promise = channel.newPromise();
            command.promise(promise);
        }
        queue.add(command);
        if (flush) {
            scheduleFlush();
        }
        return promise;
    }

    public void scheduleFlush() {
        if (scheduled.compareAndSet(false, true)) {
            channel.eventLoop().execute(this::flush);
        }
    }

    private void flush() {
        try {
            QueuedCommand cmd;
            int i = 0;
            boolean flushedOnce = false;
            while ((cmd = queue.poll()) != null) {
                cmd.run(channel);
                i++;
                if (i == DEQUE_CHUNK_SIZE) {
                    i = 0;
                    cmd.setFlush(true);
                    channel.flush();
                    flushedOnce = true;
                }
            }
            if (i != 0 || !flushedOnce) {
                channel.flush();
            }
        } finally {
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush();
            }
        }
    }

}
