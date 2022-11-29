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
package org.apache.dubbo.qos.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.apache.dubbo.common.utils.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalHostPermitHandlerTest {
    @Test
    void shouldShowIpNotPermitted_GivenAcceptForeignIpFalseAndEmptyWhiteList() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(ByteBuf.class))).thenReturn(future);
        LocalHostPermitHandler handler = new LocalHostPermitHandler(false, StringUtils.EMPTY_STRING);
        handler.handlerAdded(context);
        ArgumentCaptor<ByteBuf> captor = ArgumentCaptor.forClass(ByteBuf.class);
        verify(context).writeAndFlush(captor.capture());
        assertThat(new String(captor.getValue().array()), containsString("Foreign Ip Not Permitted, Consider Config It In Whitelist"));
        verify(future).addListener(ChannelFutureListener.CLOSE);
    }

    @Test
    void shouldShowIpNotPermitted_GivenAcceptForeignIpFalseAndInvalidWhiteList() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        when(addr.getHostAddress()).thenReturn("179.23.44.1");
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(ByteBuf.class))).thenReturn(future);
        LocalHostPermitHandler handler = new LocalHostPermitHandler(false, "175.23.44.1 ;  ~252.\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}");
        handler.handlerAdded(context);
        ArgumentCaptor<ByteBuf> captor = ArgumentCaptor.forClass(ByteBuf.class);
        verify(context).writeAndFlush(captor.capture());
        assertThat(new String(captor.getValue().array()), containsString("Foreign Ip Not Permitted, Consider Config It In Whitelist"));
        verify(future).addListener(ChannelFutureListener.CLOSE);
    }

    @Test
    void shouldNotShowIpNotPermitted_GivenAcceptForeignIpFalseAndValidWhiteList() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        when(addr.getHostAddress()).thenReturn("175.23.44.1");
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);

        LocalHostPermitHandler handler = new LocalHostPermitHandler(false, "175.23.44.1;~171.\\d{1,3}\\.\\d{1,2}\\.\\d{1,3}  ");
        handler.handlerAdded(context);
        verify(context, never()).writeAndFlush(any());
    }

    @Test
    void shouldNotShowIpNotPermitted_GivenAcceptForeignIpFalseAndValidWhiteListRegex() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        when(addr.getHostAddress()).thenReturn("171.33.21.6");
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);

        LocalHostPermitHandler handler = new LocalHostPermitHandler(false, "175.23.44.1; ~171.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        handler.handlerAdded(context);
        verify(context, never()).writeAndFlush(any());
    }
}
