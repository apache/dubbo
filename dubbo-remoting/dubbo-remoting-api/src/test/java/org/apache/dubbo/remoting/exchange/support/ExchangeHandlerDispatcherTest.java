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
package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.telnet.support.TelnetHandlerAdapter;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

class ExchangeHandlerDispatcherTest {

    @Test
    void test() throws Exception {
        ExchangeHandlerDispatcher exchangeHandlerDispatcher = new ExchangeHandlerDispatcher();

        ChannelHandler channelHandler = Mockito.mock(ChannelHandler.class);
        Replier replier = Mockito.mock(Replier.class);
        TelnetHandlerAdapter telnetHandlerAdapter = Mockito.mock(TelnetHandlerAdapter.class);
        exchangeHandlerDispatcher.addChannelHandler(channelHandler);
        exchangeHandlerDispatcher.addReplier(ExchangeHandlerDispatcherTest.class, replier);
        Field telnetHandlerField = exchangeHandlerDispatcher.getClass().getDeclaredField("telnetHandler");
        telnetHandlerField.setAccessible(true);
        telnetHandlerField.set(exchangeHandlerDispatcher, telnetHandlerAdapter);

        Channel channel = Mockito.mock(Channel.class);
        ExchangeChannel exchangeChannel = Mockito.mock(ExchangeChannel.class);
        exchangeHandlerDispatcher.connected(channel);
        exchangeHandlerDispatcher.disconnected(channel);
        exchangeHandlerDispatcher.sent(channel, null);
        exchangeHandlerDispatcher.received(channel, null);
        exchangeHandlerDispatcher.caught(channel, null);
        ExchangeHandlerDispatcherTest obj = new ExchangeHandlerDispatcherTest();
        exchangeHandlerDispatcher.reply(exchangeChannel, obj);
        exchangeHandlerDispatcher.telnet(channel, null);

        Mockito.verify(channelHandler, Mockito.times(1)).connected(channel);
        Mockito.verify(channelHandler, Mockito.times(1)).disconnected(channel);
        Mockito.verify(channelHandler, Mockito.times(1)).sent(channel, null);
        Mockito.verify(channelHandler, Mockito.times(1)).received(channel, null);
        Mockito.verify(channelHandler, Mockito.times(1)).caught(channel, null);
        Mockito.verify(replier, Mockito.times(1)).reply(exchangeChannel, obj);
        Mockito.verify(telnetHandlerAdapter, Mockito.times(1)).telnet(channel, null);

        exchangeHandlerDispatcher.removeChannelHandler(channelHandler);
        exchangeHandlerDispatcher.removeReplier(ExchangeHandlerDispatcherTest.class);
    }
}
