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

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TripleWriteQueueTest {

    private static final Runnable NOOP = () -> {};
    private static final Runnable FAIL = () -> {
        throw new IllegalStateException("boom");
    };

    private static class TestCommand extends QueuedCommand {
        private final Runnable action;

        TestCommand(Channel channel, Runnable action) {
            this.action = action;
            channel(channel);
            promise(new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE));
        }

        @Override
        public void run(Channel channel) {
            action.run();
            promise().trySuccess();
        }

        @Override
        public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {}
    }

    /** Enqueues every command before the batch runs, then runs the single scheduled batch. */
    private static void runAsOneBatch(TripleWriteQueue queue, TestCommand... commands) {
        List<Runnable> tasks = new ArrayList<>();
        for (TestCommand command : commands) {
            queue.enqueueFuture(command, r -> tasks.add(r));
        }
        Assertions.assertFalse(tasks.isEmpty());
        for (Runnable task : new ArrayList<>(tasks)) {
            try {
                task.run();
            } catch (RuntimeException processingFailure) {
                // whether a processing failure escapes the scheduled task is not part of the contract
            }
        }
    }

    @Test
    void failingCommandInTheMiddleOnlyFailsItself() {
        Channel channel = Mockito.mock(Channel.class);
        TestCommand first = new TestCommand(channel, NOOP);
        TestCommand bad = new TestCommand(channel, FAIL);
        TestCommand last = new TestCommand(channel, NOOP);

        runAsOneBatch(new TripleWriteQueue(256), first, bad, last);

        Assertions.assertTrue(first.promise().isSuccess());
        Assertions.assertTrue(bad.promise().isDone());
        Assertions.assertFalse(bad.promise().isSuccess());
        Assertions.assertTrue(last.promise().isSuccess());
    }

    @Test
    void failingLastCommandStillFlushesEarlierWrites() {
        Channel channel = Mockito.mock(Channel.class);
        TestCommand first = new TestCommand(channel, NOOP);
        TestCommand second = new TestCommand(channel, NOOP);
        TestCommand bad = new TestCommand(channel, FAIL);

        runAsOneBatch(new TripleWriteQueue(256), first, second, bad);

        Assertions.assertTrue(first.promise().isSuccess());
        Assertions.assertTrue(second.promise().isSuccess());
        Assertions.assertTrue(bad.promise().isDone());
        Assertions.assertFalse(bad.promise().isSuccess());
        Mockito.verify(channel, Mockito.atLeastOnce()).flush();
    }
}
