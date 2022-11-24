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
package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;

/**
 * {@link HeaderExchangeServer}
 */
class HeaderExchangeServerTest {

    @Test
    void test() throws InterruptedException, RemotingException {
        RemotingServer server = Mockito.mock(RemotingServer.class);
        URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 20881);
        Mockito.when(server.getUrl()).thenReturn(url);
        Mockito.when(server.canHandleIdle()).thenReturn(false);
        HeaderExchangeServer headerExchangeServer = new HeaderExchangeServer(server);
        Assertions.assertEquals(headerExchangeServer.getServer(), server);
        Assertions.assertEquals(headerExchangeServer.getUrl(), url);

        // test getChannels() and getExchangeChannels()
        Channel channel1 = Mockito.mock(Channel.class);
        Channel channel2 = Mockito.mock(Channel.class);
        Channel exchangeChannel1 = new HeaderExchangeChannel(channel1);
        Channel exchangeChannel2 = new HeaderExchangeChannel(channel2);
        Mockito.when(channel1.getAttribute(HeaderExchangeChannel.class.getName() + ".CHANNEL")).thenReturn(exchangeChannel1);
        Mockito.when(channel2.getAttribute(HeaderExchangeChannel.class.getName() + ".CHANNEL")).thenReturn(exchangeChannel2);
        Collection<Channel> exChannels = Arrays.asList(exchangeChannel1, exchangeChannel2);
        Mockito.when(server.getChannels()).thenReturn(Arrays.asList(channel1, channel2));
        Assertions.assertEquals(headerExchangeServer.getChannels(), exChannels);
        Assertions.assertEquals(headerExchangeServer.getExchangeChannels(), exChannels);

        // test getChannel(InetSocketAddress) and getExchangeChannel(InetSocketAddress)
        InetSocketAddress address1 = Mockito.mock(InetSocketAddress.class);
        InetSocketAddress address2 = Mockito.mock(InetSocketAddress.class);
        Mockito.when(server.getChannel(Mockito.eq(address1))).thenReturn(channel1);
        Mockito.when(server.getChannel(Mockito.eq(address2))).thenReturn(channel2);
        Assertions.assertEquals(headerExchangeServer.getChannel(address1), exchangeChannel1);
        Assertions.assertEquals(headerExchangeServer.getChannel(address2), exchangeChannel2);
        Assertions.assertEquals(headerExchangeServer.getExchangeChannel(address1), exchangeChannel1);
        Assertions.assertEquals(headerExchangeServer.getExchangeChannel(address2), exchangeChannel2);

        // test send(Object message) and send(Object message, boolean sent)
        headerExchangeServer.send("test");
        Mockito.verify(server, Mockito.times(1)).send("test");
        headerExchangeServer.send("test", true);
        Mockito.verify(server, Mockito.times(1)).send("test", true);

        // test reset(URL url)
        url = url.addParameter(Constants.HEARTBEAT_KEY, 3000).addParameter(Constants.HEARTBEAT_TIMEOUT_KEY, 3000 * 3);
        headerExchangeServer.reset(url);

        // test close(int timeout)
        Mockito.when(exchangeChannel1.isConnected()).thenReturn(true);
        headerExchangeServer.close(1000);
        Mockito.verify(server, Mockito.times(1)).startClose();
        Thread.sleep(1000);
        Mockito.verify(server, Mockito.times(1)).close(1000);
        Assertions.assertThrows(RemotingException.class, () -> headerExchangeServer.send("test"));
        Assertions.assertThrows(RemotingException.class, () -> headerExchangeServer.send("test", true));
    }

}
