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

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.InetSocketAddress;

class NettyChannelTest {
    private Channel channel = Mockito.mock(Channel.class);
    private URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 8080);
    private ChannelHandler channelHandler = Mockito.mock(ChannelHandler.class);

    @Test
    void test() throws Exception {
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(channel.isActive()).thenReturn(true);
        URL url = URL.valueOf("test://127.0.0.1/test");
        ChannelHandler channelHandler = Mockito.mock(ChannelHandler.class);
        NettyChannel nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);

        Assertions.assertEquals(nettyChannel.getChannelHandler(), channelHandler);
        Assertions.assertTrue(nettyChannel.isActive());

        NettyChannel.removeChannel(channel);
        Assertions.assertFalse(nettyChannel.isActive());

        nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);
        Mockito.when(channel.isActive()).thenReturn(false);
        NettyChannel.removeChannelIfDisconnected(channel);
        Assertions.assertFalse(nettyChannel.isActive());

        nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);
        Assertions.assertFalse(nettyChannel.isConnected());

        nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);
        nettyChannel.markActive(true);
        Assertions.assertTrue(nettyChannel.isActive());

    }

    @Test
    void testAddress() {
        NettyChannel nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);
        InetSocketAddress localAddress = InetSocketAddress.createUnresolved("127.0.0.1", 8888);
        InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("127.0.0.1", 9999);
        Mockito.when(channel.localAddress()).thenReturn(localAddress);
        Mockito.when(channel.remoteAddress()).thenReturn(remoteAddress);
        Assertions.assertEquals(nettyChannel.getLocalAddress(), localAddress);
        Assertions.assertEquals(nettyChannel.getRemoteAddress(), remoteAddress);
    }

    @Test
    void testSend() throws Exception {
        Mockito.when(channel.eventLoop()).thenReturn(Mockito.mock(EventLoop.class));
        Mockito.when(channel.alloc()).thenReturn(PooledByteBufAllocator.DEFAULT);
        NettyChannel nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);
        ChannelPromise future = Mockito.mock(ChannelPromise.class);
        Mockito.when(future.await(1000)).thenReturn(true);
        Mockito.when(future.cause()).thenReturn(null);
        Mockito.when(channel.writeAndFlush(Mockito.any())).thenReturn(future);
        Mockito.when(channel.newPromise()).thenReturn(future);
        Mockito.when(future.addListener(Mockito.any())).thenReturn(future);
        nettyChannel.send("msg", true);

        NettyChannel finalNettyChannel = nettyChannel;
        Exception exception = Mockito.mock(Exception.class);
        Mockito.when(exception.getMessage()).thenReturn("future cause");
        Mockito.when(future.cause()).thenReturn(exception);
        Assertions.assertThrows(RemotingException.class, () -> {
            finalNettyChannel.send("msg", true);
        }, "future cause");

        Mockito.when(future.await(1000)).thenReturn(false);
        Mockito.when(future.cause()).thenReturn(null);
        Assertions.assertThrows(RemotingException.class, () -> {
            finalNettyChannel.send("msg", true);
        }, "in timeout(1000ms) limit");

        ChannelPromise channelPromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(channelPromise);
        Mockito.when(channelPromise.await(1000)).thenReturn(true);
        Mockito.when(channelPromise.cause()).thenReturn(null);
        Mockito.when(channelPromise.addListener(Mockito.any())).thenReturn(channelPromise);
        finalNettyChannel.send("msg", true);
        ArgumentCaptor<GenericFutureListener> listenerArgumentCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(channelPromise, Mockito.times(1)).addListener(listenerArgumentCaptor.capture());
    }

    @Test
    void testAttribute() {
        NettyChannel nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);
        nettyChannel.setAttribute("k1", "v1");
        Assertions.assertTrue(nettyChannel.hasAttribute("k1"));
        Assertions.assertEquals(nettyChannel.getAttribute("k1"), "v1");
        nettyChannel.removeAttribute("k1");
        Assertions.assertFalse(nettyChannel.hasAttribute("k1"));
    }

    @Test
    void testEquals() {
        Channel channel2 = Mockito.mock(Channel.class);
        NettyChannel nettyChannel = NettyChannel.getOrAddChannel(channel, url, channelHandler);
        NettyChannel nettyChannel2 = NettyChannel.getOrAddChannel(channel2, url, channelHandler);
        Assertions.assertEquals(nettyChannel, nettyChannel);
        Assertions.assertNotEquals(nettyChannel, nettyChannel2);
    }
}
