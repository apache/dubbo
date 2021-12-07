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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.exchange.Request;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NettyClientHandlerTest {

    @Test
    public void test() throws Exception {
        URL url = new ServiceConfigURL("dubbbo", "127.0.0.1", 20901);
        ChannelHandler handler = Mockito.mock(ChannelHandler.class);
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(channel.isActive()).thenReturn(true);

        ChannelFuture future = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(future);
        when(future.cause()).thenReturn(null);

        NettyClientHandler nettyClientHandler = new NettyClientHandler(url, handler);

        nettyClientHandler.channelActive(ctx);
        ArgumentCaptor<NettyChannel> captor = ArgumentCaptor.forClass(NettyChannel.class);
        Mockito.verify(handler, Mockito.times(1)).connected(captor.capture());

        nettyClientHandler.channelInactive(ctx);
        captor = ArgumentCaptor.forClass(NettyChannel.class);
        Mockito.verify(handler, Mockito.times(1)).disconnected(captor.capture());

        Throwable throwable = Mockito.mock(Throwable.class);
        nettyClientHandler.exceptionCaught(ctx, throwable);
        captor = ArgumentCaptor.forClass(NettyChannel.class);
        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(handler, Mockito.times(1)).caught(captor.capture(), throwableArgumentCaptor.capture());

        nettyClientHandler.channelRead(ctx, "test");
        captor = ArgumentCaptor.forClass(NettyChannel.class);
        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(handler, Mockito.times(1)).received(captor.capture(), objectArgumentCaptor.capture());

        nettyClientHandler.userEventTriggered(ctx, IdleStateEvent.READER_IDLE_STATE_EVENT);
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(requestArgumentCaptor.capture());


        Request request = new Request();
        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        nettyClientHandler.write(ctx,request,promise);
        ArgumentCaptor<GenericFutureListener> listenerArgumentCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(promise, Mockito.times(1)).addListener(listenerArgumentCaptor.capture());

    }
}
