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

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the race-guard branches of the max-connection-age tasks: the channel
 * may die between task scheduling and execution. These branches cannot be
 * reached through EmbeddedChannel lifecycle events alone (channelInactive
 * cancels the tasks first), so the guards are exercised with a mocked context.
 */
class TripleServerConnectionHandlerGuardTest {

    private EmbeddedChannel schedulerChannel;
    private ChannelHandlerContext ctx;
    private Channel channel;

    @BeforeEach
    void setUp() {
        // Real embedded event loop with a virtual clock to run scheduled tasks
        schedulerChannel = new EmbeddedChannel();
        ctx = Mockito.mock(ChannelHandlerContext.class);
        channel = Mockito.mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(ctx.executor()).thenReturn(schedulerChannel.eventLoop());
        when(ctx.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
    }

    @AfterEach
    void tearDown() {
        schedulerChannel.finishAndReleaseAll();
    }

    private void invokeOnMaxConnectionAgeReached(TripleServerConnectionHandler handler) throws Exception {
        Method method = TripleServerConnectionHandler.class.getDeclaredMethod(
                "onMaxConnectionAgeReached", ChannelHandlerContext.class);
        method.setAccessible(true);
        method.invoke(handler, ctx);
    }

    @Test
    void testAgeTaskNoOpWhenChannelAlreadyInactive() throws Exception {
        TripleServerConnectionHandler handler = new TripleServerConnectionHandler(1000L, 500L);
        // The channel died after the task was scheduled but before it ran
        when(channel.isActive()).thenReturn(false);

        invokeOnMaxConnectionAgeReached(handler);

        verify(ctx, never()).writeAndFlush(any());
        verify(channel, never()).close();
    }

    @Test
    void testGraceTaskNoOpWhenChannelDiedDuringGracePeriod() throws Exception {
        TripleServerConnectionHandler handler = new TripleServerConnectionHandler(1000L, 500L);
        // Channel is active when the age task runs: advisory GOAWAY is sent
        when(channel.isActive()).thenReturn(true);
        invokeOnMaxConnectionAgeReached(handler);
        verify(ctx).writeAndFlush(any());

        // Channel dies during the grace period, before the grace task runs
        when(channel.isActive()).thenReturn(false);
        schedulerChannel.advanceTimeBy(600, TimeUnit.MILLISECONDS);
        schedulerChannel.runScheduledPendingTasks();

        // The grace task must be a no-op: no second close attempt
        verify(channel, never()).close();
    }
}
