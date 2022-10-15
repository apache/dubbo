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

import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Deprecated
public class WriteQueue {

    static final int DEQUE_CHUNK_SIZE = 128;
    private final Queue<QueuedCommand> queue;
    private final AtomicBoolean scheduled;

    public WriteQueue() {
        queue = new ConcurrentLinkedQueue<>();
        scheduled = new AtomicBoolean(false);
    }

    public ChannelFuture enqueue(QueuedCommand command, boolean rst) {
        return enqueue(command);
    }

    public ChannelFuture enqueue(QueuedCommand command) {
        ChannelPromise promise = command.promise();
        if (promise == null) {
            Channel ch = command.channel();
            promise = ch.newPromise();
            command.promise(promise);
        }
        queue.add(command);
        scheduleFlush(command.channel());
        return promise;
    }

    public void scheduleFlush(Channel ch) {
        if (scheduled.compareAndSet(false, true)) {
            ch.parent().eventLoop().execute(this::flush);
        }
    }

    private void flush() {
        Channel ch = null;
        try {
            QueuedCommand cmd;
            int i = 0;
            boolean flushedOnce = false;
            while ((cmd = queue.poll()) != null) {
                ch = cmd.channel();
                cmd.run(ch);
                i++;
                if (i == DEQUE_CHUNK_SIZE) {
                    i = 0;
                    ch.parent().flush();
                    flushedOnce = true;
                }
            }
            if (ch != null && (i != 0 || !flushedOnce)) {
                ch.parent().flush();
            }
        } finally {
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush(ch);
            }
        }
    }

}
