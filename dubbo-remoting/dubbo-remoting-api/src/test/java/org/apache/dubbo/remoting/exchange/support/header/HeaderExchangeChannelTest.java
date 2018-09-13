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
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.util.List;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class HeaderExchangeChannelTest {

    private HeaderExchangeChannel header;
    private MockChannel channel;
    private URL url = URL.valueOf("dubbo://localhost:20880");
    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    @Before
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
        Assert.assertNotNull(ret);
    }

    @Test
    public void getOrAddChannelTest01() {
        channel = new MockChannel(){
            @Override
            public URL getUrl(){
                return url;
            }

            @Override
            public boolean isConnected(){
                return true;
            }

        };
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
        HeaderExchangeChannel ret = HeaderExchangeChannel.getOrAddChannel(channel);
        Assert.assertNotNull(ret);
        Assert.assertNotNull(channel.getAttribute(CHANNEL_KEY));
        Assert.assertEquals(channel.getAttribute(CHANNEL_KEY).getClass(), HeaderExchangeChannel.class);
    }

    @Test
    public void getOrAddChannelTest02() {
        channel = null;
        HeaderExchangeChannel ret = HeaderExchangeChannel.getOrAddChannel(channel);
        Assert.assertNull(ret);
    }


    @Test
    public void removeChannelIfDisconnectedTest() {
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
        channel.setAttribute(CHANNEL_KEY, header);
        channel.close();
        HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
    }

    @Test
    public void sendTest00() {
        boolean sent = true;
        String message = "this is a test message";
        try {
            header.close(1);
            header.send(message, sent);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RemotingException);
        }
    }

    @Test
    public void sendTest01() throws RemotingException {
        boolean sent = true;
        String message = "this is a test message";
        header.send(message, sent);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0), "this is a test message");
    }

    @Test
    public void sendTest02() throws RemotingException {
        boolean sent = true;
        int message = 1;
        header.send(message, sent);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0).getClass(), Request.class);
        Request request = (Request) objects.get(0);
        Assert.assertEquals(request.getVersion(), "2.0.2");
    }

    @Test
    public void sendTest04() throws RemotingException {
        String message = "this is a test message";
        header.send(message);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0), "this is a test message");
    }

    @Test(expected = RemotingException.class)
    public void requestTest01() throws RemotingException {
        header.close(1000);
        Object requestob = new Object();
        header.request(requestob);
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
        Assert.assertEquals(argumentCaptor.getValue().getData(), requestob);
    }

    @Test(expected = RemotingException.class)
    public void requestTest03() throws RemotingException{
        channel = new MockChannel() {
            @Override
            public void send(Object req) throws RemotingException {
                throw new RemotingException(channel.getLocalAddress(), channel.getRemoteAddress(), "throw error");
            }
        };
        header = new HeaderExchangeChannel(channel);
        Object requestob = new Object();
        header.request(requestob, 1000);
    }

    @Test
    public void isClosedTest(){
        Assert.assertFalse(header.isClosed());
    }

    @Test
    public void closeTest() {
        Assert.assertFalse(channel.isClosed());
        header.close();
        Assert.assertTrue(channel.isClosed());
    }


    @Test
    public void closeWithTimeoutTest02() {
        Assert.assertFalse(channel.isClosed());
        Request request = new Request();
        DefaultFuture.newFuture(channel, request, 100);
        header.close(100);
        //return directly
        header.close(1000);
    }


    @Test
    public void startCloseTest() {
        try {
            boolean isClosing = channel.isClosing();
            Assert.assertFalse(isClosing);
            header.startClose();
            isClosing = channel.isClosing();
            Assert.assertTrue(isClosing);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getLocalAddressTest() {
        Assert.assertNull(header.getLocalAddress());
    }

    @Test
    public void getRemoteAddressTest() {
        Assert.assertNull(header.getRemoteAddress());
    }

    @Test
    public void getUrlTest() {
        Assert.assertEquals(header.getUrl(), URL.valueOf("dubbo://localhost:20880"));
    }

    @Test
    public void isConnectedTest() {
        Assert.assertFalse(header.isConnected());
    }


    @Test
    public void getChannelHandlerTest() {
        Assert.assertNull(header.getChannelHandler());
    }

    @Test
    public void getExchangeHandlerTest() {
        Assert.assertNull(header.getExchangeHandler());
    }


    @Test
    public void getAttributeAndSetAttributeTest() {
        header.setAttribute("test", "test");
        Assert.assertEquals(header.getAttribute("test"), "test");
        Assert.assertTrue(header.hasAttribute("test"));
    }

    @Test
    public void removeAttributeTest() {
        header.setAttribute("test", "test");
        Assert.assertEquals(header.getAttribute("test"), "test");
        header.removeAttribute("test");
        Assert.assertFalse(header.hasAttribute("test"));
    }


    @Test
    public void hasAttributeTest() {
        Assert.assertFalse(header.hasAttribute("test"));
        header.setAttribute("test", "test");
        Assert.assertTrue(header.hasAttribute("test"));
    }

    @Test
    public void hashCodeTest(){
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());

        Assert.assertEquals(header.hashCode(),result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalsTest(){
        Assert.assertEquals(header, new HeaderExchangeChannel(channel));
        header = new HeaderExchangeChannel(null);
        Assert.assertNotEquals(header, new HeaderExchangeChannel(channel));
    }


    @Test
    public void toStringTest(){
        Assert.assertEquals(header.toString(),channel.toString());
    }
}

