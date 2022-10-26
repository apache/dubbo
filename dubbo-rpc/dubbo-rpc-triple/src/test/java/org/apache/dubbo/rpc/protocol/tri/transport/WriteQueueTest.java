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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import org.apache.dubbo.common.BatchExecutorQueue;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.command.FrameQueueCommand;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Error;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * {@link TripleWriteQueue}
 */
public class WriteQueueTest {
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
    public void test() throws Exception {
        List<Http2StreamFrame> ayFrame = new ArrayList<>();
        TripleWriteQueue writeQueue = new TripleWriteQueue();

        {
            DefaultHttp2HeadersFrame defaultHttp2HeadersFrame = new DefaultHttp2HeadersFrame(new DefaultHttp2Headers(), false);
            writeQueue.enqueueSoon(FrameQueueCommand.createGrpcCommand(defaultHttp2HeadersFrame).channel(channel), false);
            ayFrame.add(defaultHttp2HeadersFrame);
        }
        {
            byte[] bytes = new byte[0];
            ByteBuf buf = channel.alloc().buffer();
            buf.writeByte(0);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
            DefaultHttp2DataFrame defaultHttp2DataFrame = new DefaultHttp2DataFrame(buf, true);
            writeQueue.enqueueSoon(FrameQueueCommand.createGrpcCommand(defaultHttp2DataFrame).channel(channel), false);
            ayFrame.add(defaultHttp2DataFrame);
        }
        {
            DefaultHttp2ResetFrame defaultHttp2ResetFrame = new DefaultHttp2ResetFrame(Http2Error.CANCEL);
            writeQueue.enqueueSoon(FrameQueueCommand.createGrpcCommand(defaultHttp2ResetFrame).channel(channel), false);
            ayFrame.add(defaultHttp2ResetFrame);
        }
        {
            TriRpcStatus status = TriRpcStatus.UNKNOWN
                .withCause(new RpcException())
                .withDescription("Encode Response data error");
            ByteBuf buf = ByteBufUtil.writeUtf8(channel.alloc(), status.description);
            DefaultHttp2DataFrame defaultHttp2DataFrame = new DefaultHttp2DataFrame(buf, true);
            writeQueue.enqueueSoon(FrameQueueCommand.createGrpcCommand(new DefaultHttp2DataFrame(buf, true)).channel(channel), true);
            ayFrame.add(defaultHttp2DataFrame);
        }

        while (writeMethodCalledTimes.get() != 4) {
            Thread.sleep(50);
        }

        ArgumentCaptor<FrameQueueCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FrameQueueCommand.class);
        ArgumentCaptor<ChannelPromise> promiseArgumentCaptor = ArgumentCaptor.forClass(ChannelPromise.class);
        Mockito.verify(channel, Mockito.times(4)).write(commandArgumentCaptor.capture(), promiseArgumentCaptor.capture());
        List<FrameQueueCommand> queuedCommands = commandArgumentCaptor.getAllValues();
        Assertions.assertEquals(queuedCommands.size(), ayFrame.size());
        for (int i = 0; i < queuedCommands.size(); i++) {
            Assertions.assertTrue(queuedCommands.get(i).getFrame().equals(ayFrame.get(i)));
        }
    }

    @Test
    public void testChunk() throws Exception {
        TripleWriteQueue writeQueue = new TripleWriteQueue();
        // test deque chunk size
        writeMethodCalledTimes.set(0);
        for (int i = 0; i < BatchExecutorQueue.DEFAULT_QUEUE_SIZE; i++) {
            DefaultHttp2HeadersFrame defaultHttp2HeadersFrame = new DefaultHttp2HeadersFrame(new DefaultHttp2Headers(), false);
            writeQueue.enqueueSoon(FrameQueueCommand.createGrpcCommand(defaultHttp2HeadersFrame).channel(channel), false);
        }
        DefaultHttp2HeadersFrame defaultHttp2HeadersFrame = new DefaultHttp2HeadersFrame(new DefaultHttp2Headers(), false);
        writeQueue.enqueueSoon(FrameQueueCommand.createGrpcCommand(defaultHttp2HeadersFrame).channel(channel), true);

        while (writeMethodCalledTimes.get() != (BatchExecutorQueue.DEFAULT_QUEUE_SIZE + 1)) {
            Thread.sleep(50);
        }
        Mockito.verify(channel, Mockito.times(BatchExecutorQueue.DEFAULT_QUEUE_SIZE + 1)).write(Mockito.any(), Mockito.any());
    }

}
