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


import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link TripleHttp2ClientResponseHandler }
 */
class TripleHttp2ClientResponseHandlerTest {
    private TripleHttp2ClientResponseHandler handler;
    private ChannelHandlerContext ctx;
    private AbstractH2TransportListener transportListener;


    @BeforeEach
    public void init() {
        transportListener = Mockito.mock(AbstractH2TransportListener.class);
        handler = new TripleHttp2ClientResponseHandler(transportListener);
        ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(ctx.channel()).thenReturn(channel);
    }

    @Test
    void testUserEventTriggered() throws Exception {
        // test Http2GoAwayFrame
        Http2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(Http2Error.NO_ERROR, ByteBufUtil
                .writeAscii(ByteBufAllocator.DEFAULT, "app_requested"));
        handler.userEventTriggered(ctx, goAwayFrame);
        Mockito.verify(ctx, Mockito.times(1)).close();

        // test Http2ResetFrame
        DefaultHttp2ResetFrame resetFrame = new DefaultHttp2ResetFrame(Http2Error.CANCEL);
        handler.userEventTriggered(ctx, resetFrame);
        Mockito.verify(ctx, Mockito.times(2)).close();
    }

    @Test
    void testChannelRead0() throws Exception {
        final Http2Headers headers = new DefaultHttp2Headers(true);
        DefaultHttp2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, true);
        handler.channelRead0(ctx, headersFrame);
        Mockito.verify(transportListener, Mockito.times(1)).onHeader(headers, true);
    }

    @Test
    void testExceptionCaught() {
        RuntimeException exception = new RuntimeException();
        handler.exceptionCaught(ctx, exception);
        Mockito.verify(ctx).close();
        Mockito.verify(transportListener).cancelByRemote(Http2Error.INTERNAL_ERROR.code());
    }
}
