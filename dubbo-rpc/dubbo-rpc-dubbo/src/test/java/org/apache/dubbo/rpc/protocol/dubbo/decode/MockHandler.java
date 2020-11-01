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
package org.apache.dubbo.rpc.protocol.dubbo.decode;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class MockHandler extends ChannelDuplexHandler {
    private final Consumer consumer;

    private final ChannelHandler handler;

    public MockHandler(Consumer consumer, ChannelHandler handler) {
        this.consumer = consumer;
        this.handler = handler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(channel.getRemoteAddress()).thenAnswer(invo -> new InetSocketAddress(NetUtils.getAvailablePort()));
        Mockito.when(channel.getUrl()).thenAnswer(invo -> new URL("dubbo", "localhost", 20880));
        Mockito.when(channel.getLocalAddress()).thenAnswer(invo -> new InetSocketAddress(20883));
        try {
            Mockito.doAnswer(invo -> {
                if (consumer != null) {
                    consumer.accept(invo.getArgument(0));
                }
                return null;
            }).when(channel).send(Mockito.any());
        } catch (RemotingException e) {
            e.printStackTrace();
        }
        this.handler.received(channel, msg);
    }
}
