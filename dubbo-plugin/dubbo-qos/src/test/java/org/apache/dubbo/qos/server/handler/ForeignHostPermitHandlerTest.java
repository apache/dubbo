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
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.qos.api.QosConfiguration;
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

class ForeignHostPermitHandlerTest {
    @Test
    void shouldShowIpNotPermittedMsg_GivenAcceptForeignIpFalseAndEmptyWhiteList() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(ByteBuf.class))).thenReturn(future);
        ForeignHostPermitHandler handler = new ForeignHostPermitHandler(
            QosConfiguration.builder()
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist(StringUtils.EMPTY_STRING)
                .anonymousAccessPermissionLevel(PermissionLevel.NONE.name())
                .build()
        );
        handler.handlerAdded(context);
        ArgumentCaptor<ByteBuf> captor = ArgumentCaptor.forClass(ByteBuf.class);
        verify(context).writeAndFlush(captor.capture());
        assertThat(new String(captor.getValue().array()), containsString("Foreign Ip Not Permitted, Consider Config It In Whitelist"));
        verify(future).addListener(ChannelFutureListener.CLOSE);
    }

    @Test
    void shouldShowIpNotPermittedMsg_GivenAcceptForeignIpFalseAndNotMatchWhiteList() throws Exception {
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
        ForeignHostPermitHandler handler = new ForeignHostPermitHandler(
            QosConfiguration.builder()
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist("175.23.44.1 ,  192.168.1.192/26")
                .anonymousAccessPermissionLevel(PermissionLevel.NONE.name())
                .build()
        );

        handler.handlerAdded(context);
        ArgumentCaptor<ByteBuf> captor = ArgumentCaptor.forClass(ByteBuf.class);
        verify(context).writeAndFlush(captor.capture());
        assertThat(new String(captor.getValue().array()), containsString("Foreign Ip Not Permitted, Consider Config It In Whitelist"));
        verify(future).addListener(ChannelFutureListener.CLOSE);
    }

    @Test
    void shouldNotShowIpNotPermittedMsg_GivenAcceptForeignIpFalseAndMatchWhiteList() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        when(addr.getHostAddress()).thenReturn("175.23.44.1");
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);

        ForeignHostPermitHandler handler = new ForeignHostPermitHandler(
            QosConfiguration.builder()
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist("175.23.44.1, 192.168.1.192/26  ")
                .build()
        );
        handler.handlerAdded(context);
        verify(context, never()).writeAndFlush(any());
    }

    @Test
    void shouldNotShowIpNotPermittedMsg_GivenAcceptForeignIpFalseAndMatchWhiteListRange() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        when(addr.getHostAddress()).thenReturn("192.168.1.199");
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);

        ForeignHostPermitHandler handler = new ForeignHostPermitHandler(
            QosConfiguration.builder()
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist("175.23.44.1, 192.168.1.192/26")
                .build()
        );
        handler.handlerAdded(context);
        verify(context, never()).writeAndFlush(any());
    }

    @Test
    void shouldNotShowIpNotPermittedMsg_GivenAcceptForeignIpFalseAndNotMatchWhiteListAndPermissionConfig() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(ByteBuf.class))).thenReturn(future);
        ForeignHostPermitHandler handler = new ForeignHostPermitHandler(
            QosConfiguration.builder()
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist("175.23.44.1 ,  192.168.1.192/26")
                .anonymousAccessPermissionLevel(PermissionLevel.PROTECTED.name())
                .build()
        );

        handler.handlerAdded(context);
        verify(future, never()).addListener(ChannelFutureListener.CLOSE);
    }
}
