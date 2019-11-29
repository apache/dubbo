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
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeaderExchangeChannelTest {

    private HeaderExchangeChannel header;
    private MockChannel channel;
    private URL url = URL.valueOf("dubbo://localhost:20880");
    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    @BeforeEach
    public void setup() {
        channel = new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }
        };

        header = new HeaderExchangeChannel(channel);
    }

    @Test
    public void getOrAddChannelTest00() {
        channel.setAttribute("CHANNEL_KEY", "attribute");
        HeaderExchangeChannel ret = HeaderExchangeChannel.getOrAddChannel(channel);
        Assertions.assertNotNull(ret);
    }

    @Test
    public void getOrAddChannelTest01() {
        channel = new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }

            @Override
            public boolean isConnected() {
                return true;
            }

        };
        Assertions.assertNull(channel.getAttribute(CHANNEL_KEY));
        HeaderExchangeChannel ret = HeaderExchangeChannel.getOrAddChannel(channel);
        Assertions.assertNotNull(ret);
        Assertions.assertNotNull(channel.getAttribute(CHANNEL_KEY));
        Assertions.assertEquals(channel.getAttribute(CHANNEL_KEY).getClass(), HeaderExchangeChannel.class);
    }

    @Test
    public void getOrAddChannelTest02() {
        channel = null;
        HeaderExchangeChannel ret = HeaderExchangeChannel.getOrAddChannel(channel);
        Assertions.assertNull(ret);
    }


    @Test
    public void removeChannelIfDisconnectedTest() {
        Assertions.assertNull(channel.getAttribute(CHANNEL_KEY));
        channel.setAttribute(CHANNEL_KEY, header);
        channel.close();
        HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        Assertions.assertNull(channel.getAttribute(CHANNEL_KEY));
    }

    @Test
    public void sendTest00() {
        boolean sent = true;
        String message = "this is a test message";
        try {
            header.close(1);
            header.send(message, sent);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RemotingException);
        }
    }

    @Test
    public void sendTest01() throws RemotingException {
        boolean sent = true;
        String message = "this is a test message";
        header.send(message, sent);
        List<Object> objects = channel.getSentObjects();
        Assertions.assertEquals(objects.get(0), "this is a test message");
    }

    @Test
    public void sendTest02() throws RemotingException {
        boolean sent = true;
        int message = 1;
        header.send(message, sent);
        List<Object> objects = channel.getSentObjects();
        Assertions.assertEquals(objects.get(0).getClass(), Request.class);
        Request request = (Request) objects.get(0);
        Assertions.assertEquals(request.getVersion(), "2.0.2");
    }

    @Test
    public void sendTest04() throws RemotingException {
        String message = "this is a test message";
        header.send(message);
        List<Object> objects = channel.getSentObjects();
        Assertions.assertEquals(objects.get(0), "this is a test message");
    }

    @Test
    public void requestTest01() throws RemotingException {
        Assertions.assertThrows(RemotingException.class, () -> {
            header.close(1000);
            Object requestob = new Object();
            header.request(requestob);
        });
    }

    @Test
    public void requestTest02() throws RemotingException {
        Channel channel = Mockito.mock(MockChannel.class);
        header = new HeaderExchangeChannel(channel);
        when(channel.getUrl()).thenReturn(url);
        Object requestob = new Object();
        header.request(requestob);
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(channel, times(1)).send(argumentCaptor.capture());
        Assertions.assertEquals(argumentCaptor.getValue().getData(), requestob);
    }

    @Test
    public void requestTest03() throws RemotingException {
        Assertions.assertThrows(RemotingException.class, () -> {
            channel = new MockChannel() {
                @Override
                public void send(Object req) throws RemotingException {
                    throw new RemotingException(channel.getLocalAddress(), channel.getRemoteAddress(), "throw error");
                }
            };
            header = new HeaderExchangeChannel(channel);
            Object requestob = new Object();
            header.request(requestob, 1000);
        });
    }

    @Test
    public void isClosedTest() {
        Assertions.assertFalse(header.isClosed());
    }

    @Test
    public void closeTest() {
        Assertions.assertFalse(channel.isClosed());
        header.close();
        Assertions.assertTrue(channel.isClosed());
    }


    @Test
    public void closeWithTimeoutTest02() {
        Assertions.assertFalse(channel.isClosed());
        Request request = new Request();
        DefaultFuture.newFuture(channel, request, 100, null);
        header.close(100);
        //return directly
        header.close(1000);
    }


    @Test
    public void startCloseTest() {
        try {
            boolean isClosing = channel.isClosing();
            Assertions.assertFalse(isClosing);
            header.startClose();
            isClosing = channel.isClosing();
            Assertions.assertTrue(isClosing);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getLocalAddressTest() {
        Assertions.assertNull(header.getLocalAddress());
    }

    @Test
    public void getRemoteAddressTest() {
        Assertions.assertNull(header.getRemoteAddress());
    }

    @Test
    public void getUrlTest() {
        Assertions.assertEquals(header.getUrl(), URL.valueOf("dubbo://localhost:20880"));
    }

    @Test
    public void isConnectedTest() {
        Assertions.assertFalse(header.isConnected());
    }


    @Test
    public void getChannelHandlerTest() {
        Assertions.assertNull(header.getChannelHandler());
    }

    @Test
    public void getExchangeHandlerTest() {
        Assertions.assertNull(header.getExchangeHandler());
    }


    @Test
    public void getAttributeAndSetAttributeTest() {
        header.setAttribute("test", "test");
        Assertions.assertEquals(header.getAttribute("test"), "test");
        Assertions.assertTrue(header.hasAttribute("test"));
    }

    @Test
    public void removeAttributeTest() {
        header.setAttribute("test", "test");
        Assertions.assertEquals(header.getAttribute("test"), "test");
        header.removeAttribute("test");
        Assertions.assertFalse(header.hasAttribute("test"));
    }


    @Test
    public void hasAttributeTest() {
        Assertions.assertFalse(header.hasAttribute("test"));
        header.setAttribute("test", "test");
        Assertions.assertTrue(header.hasAttribute("test"));
    }

    @Test
    public void hashCodeTest() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());

        Assertions.assertEquals(header.hashCode(), result);
    }

    @Test
    public void equalsTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Assertions.assertEquals(header, new HeaderExchangeChannel(channel));
            header = new HeaderExchangeChannel(null);
            Assertions.assertNotEquals(header, new HeaderExchangeChannel(channel));
        });
    }


    @Test
    public void toStringTest() {
        Assertions.assertEquals(header.toString(), channel.toString());
    }
}

