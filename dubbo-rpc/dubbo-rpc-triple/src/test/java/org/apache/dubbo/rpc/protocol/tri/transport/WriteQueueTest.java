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

import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;
import org.apache.dubbo.rpc.protocol.tri.command.TextDataQueueCommand;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Error;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.rpc.protocol.tri.transport.WriteQueue.DEQUE_CHUNK_SIZE;


/**
 * {@link WriteQueue}
 */
class WriteQueueTest {
    private final AtomicInteger writeMethodCalledTimes = new AtomicInteger(0);
    private Channel channel;

    @BeforeEach
    public void init() {
        channel = Mockito.mock(Channel.class);
        Channel parent = Mockito.mock(Channel.class);
        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        EventLoop eventLoop = new DefaultEventLoop();
        Mockito.when(parent.eventLoop()).thenReturn(eventLoop);

        Mockito.when(channel.parent()).thenReturn(parent);
        Mockito.when(channel.eventLoop()).thenReturn(eventLoop);
        Mockito.when(channel.isActive()).thenReturn(true);
        Mockito.when(channel.newPromise()).thenReturn(promise);
        Mockito.when(channel.write(Mockito.any(), Mockito.any())).thenAnswer(
                (Answer<ChannelPromise>) invocationOnMock -> {
                    writeMethodCalledTimes.incrementAndGet();
                    return promise;
                });

        writeMethodCalledTimes.set(0);
    }

    @Test
    @Disabled
    void test() throws Exception {

        WriteQueue writeQueue = new WriteQueue();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        TripleStreamChannelFuture tripleStreamChannelFuture = new TripleStreamChannelFuture(embeddedChannel);
        writeQueue.enqueue(HeaderQueueCommand.createHeaders(tripleStreamChannelFuture, new DefaultHttp2Headers()).channel(channel));
        writeQueue.enqueue(DataQueueCommand.create(tripleStreamChannelFuture, new byte[0], false, 0).channel(channel));
        TriRpcStatus status = TriRpcStatus.UNKNOWN
                .withCause(new RpcException())
                .withDescription("Encode Response data error");
        writeQueue.enqueue(CancelQueueCommand.createCommand(tripleStreamChannelFuture, Http2Error.CANCEL).channel(channel));
        writeQueue.enqueue(TextDataQueueCommand.createCommand(tripleStreamChannelFuture, status.description, true).channel(channel));

        while (writeMethodCalledTimes.get() != 4) {
            Thread.sleep(50);
        }

        ArgumentCaptor<QueuedCommand> commandArgumentCaptor = ArgumentCaptor.forClass(QueuedCommand.class);
        ArgumentCaptor<ChannelPromise> promiseArgumentCaptor = ArgumentCaptor.forClass(ChannelPromise.class);
        Mockito.verify(channel, Mockito.times(4)).write(commandArgumentCaptor.capture(), promiseArgumentCaptor.capture());
        List<QueuedCommand> queuedCommands = commandArgumentCaptor.getAllValues();
        Assertions.assertEquals(queuedCommands.size(), 4);
        Assertions.assertTrue(queuedCommands.get(0) instanceof HeaderQueueCommand);
        Assertions.assertTrue(queuedCommands.get(1) instanceof DataQueueCommand);
        Assertions.assertTrue(queuedCommands.get(2) instanceof CancelQueueCommand);
        Assertions.assertTrue(queuedCommands.get(3) instanceof TextDataQueueCommand);
    }

    @Test
    @Disabled
    void testChunk() throws Exception {
        WriteQueue writeQueue = new WriteQueue();
        // test deque chunk size
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        TripleStreamChannelFuture tripleStreamChannelFuture = new TripleStreamChannelFuture(embeddedChannel);
        writeMethodCalledTimes.set(0);
        for (int i = 0; i < DEQUE_CHUNK_SIZE; i++) {
            writeQueue.enqueue(HeaderQueueCommand.createHeaders(tripleStreamChannelFuture, new DefaultHttp2Headers()).channel(channel));
        }
        writeQueue.enqueue(HeaderQueueCommand.createHeaders(tripleStreamChannelFuture, new DefaultHttp2Headers()).channel(channel));
        while (writeMethodCalledTimes.get() != (DEQUE_CHUNK_SIZE + 1)) {
            Thread.sleep(50);
        }
        Mockito.verify(channel, Mockito.times(DEQUE_CHUNK_SIZE + 1)).write(Mockito.any(), Mockito.any());
    }

}
