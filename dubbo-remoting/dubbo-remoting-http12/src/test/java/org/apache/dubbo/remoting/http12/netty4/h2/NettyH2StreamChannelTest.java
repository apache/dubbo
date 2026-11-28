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
package org.apache.dubbo.remoting.http12.netty4.h2;

import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.Http2StreamChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NettyH2StreamChannelTest {
    @Test
    void streamLookupAndFlowControlRunTogetherOnEventLoop() throws Exception {
        verifyDeferredConsumption(false);
    }

    @Test
    void retiredStreamIsLookedUpAfterQueuedCallbackStarts() throws Exception {
        verifyDeferredConsumption(true);
    }

    private static void verifyDeferredConsumption(boolean retired) throws Exception {
        Http2Connection connection = mock(Http2Connection.class, RETURNS_DEEP_STUBS);
        Http2StreamChannel channel = mock(Http2StreamChannel.class);
        Http2FrameStream frameStream = mock(Http2FrameStream.class);
        EventLoop loop = mock(EventLoop.class);
        Http2Stream stream = mock(Http2Stream.class);
        when(channel.stream()).thenReturn(frameStream);
        when(frameStream.id()).thenReturn(3);
        when(channel.eventLoop()).thenReturn(loop);
        when(connection.stream(3)).thenReturn(stream);
        AtomicReference<Runnable> pending = new AtomicReference<>();
        doAnswer(call -> {
                    pending.set(call.getArgument(0));
                    return null;
                })
                .when(loop)
                .execute(any(Runnable.class));
        NettyH2StreamChannel transport = new NettyH2StreamChannel(channel, null, connection);
        transport.consumeBytes(10);
        verify(connection, never()).stream(3);
        assertNotNull(pending.get());
        if (retired) {
            when(connection.stream(3)).thenReturn(null);
        }
        pending.get().run();
        verify(connection).stream(3);
        verify(connection.local().flowController(), retired ? never() : times(1))
                .consumeBytes(stream, 10);
    }
}
