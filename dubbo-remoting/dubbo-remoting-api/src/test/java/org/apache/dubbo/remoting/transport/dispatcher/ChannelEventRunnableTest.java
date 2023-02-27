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
package org.apache.dubbo.remoting.transport.dispatcher;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * {@link ChannelEventRunnable}
 */
class ChannelEventRunnableTest {

    @Test
    void test() throws Exception {
        ChannelEventRunnable.ChannelState[] values = ChannelEventRunnable.ChannelState.values();
        Assertions.assertEquals(Arrays.toString(values), "[CONNECTED, DISCONNECTED, SENT, RECEIVED, CAUGHT]");

        Channel channel = Mockito.mock(Channel.class);
        ChannelHandler handler = Mockito.mock(ChannelHandler.class);
        ChannelEventRunnable connectRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.CONNECTED);
        ChannelEventRunnable disconnectRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.DISCONNECTED);
        ChannelEventRunnable sentRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.SENT);
        ChannelEventRunnable receivedRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.RECEIVED, "");
        ChannelEventRunnable caughtRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.CAUGHT, new RuntimeException());

        connectRunnable.run();
        disconnectRunnable.run();
        sentRunnable.run();
        receivedRunnable.run();
        caughtRunnable.run();

        ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);
        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(handler, Mockito.times(1)).connected(channelArgumentCaptor.capture());
        Mockito.verify(handler, Mockito.times(1)).disconnected(channelArgumentCaptor.capture());
        Mockito.verify(handler, Mockito.times(1)).sent(channelArgumentCaptor.capture(), Mockito.any());
        Mockito.verify(handler, Mockito.times(1)).received(channelArgumentCaptor.capture(), objectArgumentCaptor.capture());
        Mockito.verify(handler, Mockito.times(1)).caught(channelArgumentCaptor.capture(), throwableArgumentCaptor.capture());
    }
}
