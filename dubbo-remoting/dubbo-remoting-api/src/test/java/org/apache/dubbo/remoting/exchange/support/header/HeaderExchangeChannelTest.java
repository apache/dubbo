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
import org.junit.Assert;
import org.junit.Test;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.handler.MockedChannel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.util.List;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class HeaderExchangeChannelTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;
    private HeaderExchangeChannel header;
    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    private MockChannel channelConnectedTrue;

    private HeaderExchangeChannel defaultChannel() {
        Channel channel = new MockedChannel();
        return new HeaderExchangeChannel(channel);
    }

    private void initHeader(){

        header = new HeaderExchangeChannel(channel);
    }

    private void initChannel(){
        channel = new MockChannel() {

            @Override
            public URL getUrl() {
                return url;
            }
        };


        channelConnectedTrue = new MockChannel(){
            @Override
            public URL getUrl(){
                return url;
            }

            @Override
            public boolean isConnected(){
                return true;
            }

        };
    }



    @Test(expected = IllegalArgumentException.class)
    public void headerExchangeChannelTest(){
        HeaderExchangeChannel test = new HeaderExchangeChannel(null);
    }

    @Test
    public void getOrAddChannelTest00() {
        initChannel();
        initHeader();
        channel.setAttribute("CHANNEL_KEY", "attribute");
        HeaderExchangeChannel ret = header.getOrAddChannel(channel);
        Assert.assertNotNull(ret);
    }

    @Test
    public void getOrAddChannelTest01() {
        initChannel();
        initHeader();
        Assert.assertNull(channelConnectedTrue.getAttribute(CHANNEL_KEY));
        HeaderExchangeChannel ret = HeaderExchangeChannel.getOrAddChannel(channelConnectedTrue);
        Assert.assertNotNull(ret);
        Assert.assertNotNull(channelConnectedTrue.getAttribute(CHANNEL_KEY));
        Assert.assertEquals(channelConnectedTrue.getAttribute(CHANNEL_KEY).getClass(), HeaderExchangeChannel.class);
    }

    @Test
    public void getOrAddChannelTest02() {
        Channel channel1 = null;
        HeaderExchangeChannel ret = header.getOrAddChannel(channel1);
        Assert.assertNull(ret);
    }

    @Test
    public void getOrAddChannelTest() {
        initChannel();
        HeaderExchangeChannel testChWhetherNull = HeaderExchangeChannel.getOrAddChannel(null);
        Assert.assertNull("params ch is null",testChWhetherNull);
        testChWhetherNull = HeaderExchangeChannel.getOrAddChannel(channelConnectedTrue);
        HeaderExchangeChannel testRetWhetherNull =  (HeaderExchangeChannel) channelConnectedTrue.getAttribute(CHANNEL_KEY);
        Assert.assertSame("ret is null",testChWhetherNull,testRetWhetherNull);
    }



    @Test
    public void removeChannelIfDisconnectedTest() {
        initChannel();
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
        initHeader();
        channel.setAttribute(CHANNEL_KEY, header);
        channel.close();
        HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
    }

    @Test
    public void sendTest00() {
        initChannel();
        initHeader();
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
        initChannel();
        initHeader();
        boolean sent = true;
        String message = "this is a test message";
        header.send(message, sent);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0), "this is a test message");
    }

    @Test
    public void sendTest02() throws RemotingException {
        initChannel();
        initHeader();
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
        initChannel();
        initHeader();
        String message = "this is a test message";
        header.send(message);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0), "this is a test message");
    }

    @Test(expected = RemotingException.class)
    public void requestTest() throws RemotingException {
        Channel channel = Mockito.mock(MockChannel.class);
        header = new HeaderExchangeChannel(channel);
        when(channel.getUrl()).thenReturn(url);
        Object requestob = new Object();
        header.request(requestob);
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(channel, times(1)).send(argumentCaptor.capture());
        Assert.assertEquals(argumentCaptor.getValue().getData(), requestob);

        initChannel();
        initHeader();
        header.close(1000);
        header.request(requestob);
    }

    @Test
    public void isClosedTest(){
        initChannel();
        initHeader();
        Assert.assertFalse(header.isClosed());
    }

    @Test
    public void closeTest() {
        initChannel();
        initHeader();

        Assert.assertFalse(channel.isClosed());
        header.close();
        Assert.assertTrue(channel.isClosed());
    }

    @Test
    public void closeWithTimeoutTest() {
        initChannel();
        initHeader();
        Assert.assertFalse(channel.isClosed());

        header.close(100);
    }


    @Test
    public void startCloseTest() {
        initChannel();
        initHeader();
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
        initChannel();
        initHeader();
        Assert.assertNull(header.getLocalAddress());
    }

    @Test
    public void getRemoteAddressTest() {
        initChannel();
        initHeader();
        Assert.assertNull(header.getRemoteAddress());
    }

    @Test
    public void getUrlTest() {
        initChannel();
        initHeader();
        Assert.assertEquals(header.getUrl(), URL.valueOf("dubbo://localhost:20880"));
    }

    @Test
    public void isConnectedTest() {
        initChannel();
        initHeader();
        Assert.assertFalse(header.isConnected());
    }


    @Test
    public void getChannelHandlerTest() {
        initChannel();
        initHeader();
        Assert.assertNull(header.getChannelHandler());
    }

    @Test
    public void getExchangeHandlerTest() {
        initChannel();
        initHeader();
        Assert.assertNull(header.getChannelHandler());
    }


    @Test
    public void getAttributeAndSetAttributeTest() {
        initChannel();
        initHeader();
        header.setAttribute("test", "test");
        Assert.assertEquals(header.getAttribute("test"), "test");
        Assert.assertTrue(header.hasAttribute("test"));
    }

    @Test
    public void removeAttributeTest() {
        initChannel();
        initHeader();
        header.setAttribute("test", "test");
        Assert.assertEquals(header.getAttribute("test"), "test");
        header.removeAttribute("test");
        Assert.assertFalse(header.hasAttribute("test"));
    }


    @Test
    public void hasAttributeTest() {
        initChannel();
        initHeader();
        Assert.assertFalse(header.hasAttribute("test"));
        header.setAttribute("test", "test");
        Assert.assertTrue(header.hasAttribute("test"));
    }

    @Test
    public void hashCodeTest(){
        initChannel();
        initHeader();

        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());

        Assert.assertEquals(header.hashCode(),result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalsTest(){
        initChannel();
        initHeader();
        Assert.assertEquals(header, new HeaderExchangeChannel(channel));
        header = new HeaderExchangeChannel(null);
        Assert.assertNotEquals(header, new HeaderExchangeChannel(channelConnectedTrue));
    }


    @Test
    public void toStringTest(){
        initChannel();
        initHeader();
        Assert.assertEquals(header.toString(),channel.toString());
    }
}

