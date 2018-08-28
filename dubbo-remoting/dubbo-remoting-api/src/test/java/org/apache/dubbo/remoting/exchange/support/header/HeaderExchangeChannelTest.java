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
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HeaderExchangeChannelTest {
    private boolean inSend;
    private boolean isSent;
    private boolean isRequest;

    private boolean isClosed;
    private volatile boolean closing;

    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    @Before
    public void setUp() {
        inSend = false;
        isSent = true;
        isRequest = false;
        isClosed = false;
        closing = false;
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_error_null() {
        new HeaderExchangeChannel(null);
    }

    @Test
    public void test_send_oneway_error_close() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        channel.close();
        try {
            channel.send(new Object());
        } catch (Exception e) {
            assertTrue(e instanceof RemotingException);
            assertTrue(e.getMessage().contains("Failed to send message"));
        }
    }


    @Test
    public void test_send_oneway_request_successful() throws RemotingException {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        Response response = new Response();
        String string = new String();

        channel.send(response);
        assertTrue(!isSent);
        assertTrue(inSend);
        assertTrue(!isRequest);
        inSend = false;
        isRequest = false;
        channel.send(string);
        assertTrue(!isSent);
        assertTrue(inSend);
        assertTrue(!isRequest);
        inSend = false;
        isRequest = false;
    }

    @Test
    public void remove_attribute_successful() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        channel.setAttribute("test", 123);
        channel.removeAttribute("test");
        assertTrue(channel.getAttribute("test") == null);
    }

    @Test
    public void remove_channel_if_disconnected() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        assertTrue(channel != null);
        assertTrue(!channel.isConnected());
        channel.removeChannelIfDisconnected(channel);
        assertTrue(channel.getAttribute(CHANNEL_KEY) == null);
    }

    @Test
    public void get_attribute_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        channel.setAttribute("test", 123);
        Object result = channel.getAttribute("test");
        assertTrue(result != null);
    }

    @Test
    public void test_request_error() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        Object request = new Object();
        int timeout = 1000;
        try {
            channel.request(request, timeout);
        } catch (Exception e) {
            assertTrue(e instanceof RemotingException);
            assertTrue(e.getMessage().contains("Failed to send message"));
        }
    }

    @Test
    public void test_request_successful() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        Object request = new Object();
        int timeout = 1000;
        ResponseFuture future = null;
        try {
            future = channel.request(request, timeout);
        } catch (Exception e) {
            assertTrue(e instanceof RemotingException);
            assertTrue(e.getMessage().contains("Failed to send message"));
        }
        assertTrue(future != null);
    }

    @Test
    public void start_close_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        channel.startClose();
        assertTrue(closing == true);
    }

    @Test
    public void get_local_address_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        InetSocketAddress result = channel.getLocalAddress();
        assertTrue(result != null);
    }

    @Test
    public void get_remote_address_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        InetSocketAddress result = channel.getRemoteAddress();
        assertTrue(result == null);
    }

    @Test
    public void get_url_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        URL result = channel.getUrl();
        assertTrue(result == null);
    }

    @Test
    public void is_connected_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        Boolean result = channel.isConnected();
        assertTrue(result == false);
    }

    @Test
    public void get_channel_handler_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        ChannelHandler result = channel.getChannelHandler();
        assertTrue(result == null);
    }

    @Test
    public void get_exchange_handler_test() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        ExchangeHandler result = channel.getExchangeHandler();
        assertTrue(result == null);
    }

    @Test
    public void test_send_oneway_non_request_successful() throws RemotingException {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());

        channel.send(new Object());
        //assertTrue(!isSent);
        //assertTrue(inSend);
        assertTrue(isRequest);
    }

    @Test
    public void test_send_twoway_error_close() {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        channel.close();
        try {
            channel.send(new Object(), true);
        } catch (Exception e) {
            assertTrue(e instanceof RemotingException);
            assertTrue(e.getMessage().contains("Failed to send message"));
        }
    }

    @Test
    public void test_send_twoway_request_successful() throws RemotingException {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());
        Response response = new Response();
        String string = new String();

        channel.send(response, true);
        assertTrue(isSent);
        assertTrue(inSend);
        assertTrue(!isRequest);
        inSend = false;
        isRequest = false;
        isSent = false;
        channel.send(string, true);
        assertTrue(isSent);
        assertTrue(inSend);
        assertTrue(!isRequest);
        inSend = false;
        isSent = false;
        isRequest = false;
    }

    @Test
    public void test_send_twoway_non_request_successful() throws RemotingException {
        HeaderExchangeChannel channel = new HeaderExchangeChannel(getChannel());

        channel.send(new Object(), true);
        //assertTrue(!isSent);
        //assertTrue(inSend);
        assertTrue(isRequest);
    }

    @Test
    public void test_get_or_add_channel_null() {
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(null);
        assertEquals(null, channel);
    }

    @Test
    public void test_get_or_add_channel_successful() {
        String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";
        Channel cal = getChannel();
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(cal);
        assertTrue(channel != null);
        assertTrue(channel.getAttribute(CHANNEL_KEY) == null);

    }

    @Test
    public void test_close() {
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(getChannel());
        channel.close();
        assertTrue(!channel.isClosed());
        assertTrue(isClosed);
    }

    @Test
    public void test_close_timeout_close_again() {
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(getChannel());
        channel.close();

        int timeout = 5000;
        long start = System.currentTimeMillis();
        channel.close(timeout);
        long end = System.currentTimeMillis();

        assertTrue(channel.isClosed());
        assertTrue(end - start <= timeout);
    }

    @Test
    public void test_close_timeout_first_time_non_default_future() {
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(getChannel());


        int timeout = 5000;
        long start = System.currentTimeMillis();
        channel.close(timeout);
        long end = System.currentTimeMillis();

        assertTrue(channel.isClosed());
        assertTrue(end - start <= timeout);
    }

    @Test
    public void test_close_timeout_first_time_has_default_future() throws RemotingException {
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(getChannel());

        int timeout = 5000;
        channel.request(new Request(), timeout);
        long start = System.currentTimeMillis();
        channel.close(timeout);
        long end = System.currentTimeMillis();

        assertTrue(channel.isClosed());
        assertTrue(end - start >= timeout);
    }

    @Test
    public void test_isconnected() {
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(getChannel());
        assertTrue(!channel.isConnected());
    }

    @Test
    public void test_hashcode() {
        HeaderExchangeChannel channel = HeaderExchangeChannel.getOrAddChannel(getChannel());
        assertTrue(channel.hashCode() != 0);
    }

    @Test
    public void test_equals() {
        Channel cal1 = getChannel();
        Channel cal2 = getChannel();
        HeaderExchangeChannel channel1 = HeaderExchangeChannel.getOrAddChannel(cal1);
        HeaderExchangeChannel channel2 = HeaderExchangeChannel.getOrAddChannel(cal2);
        HeaderExchangeChannel channel3 = HeaderExchangeChannel.getOrAddChannel(cal1);

        assertTrue(!channel1.equals(null));
        assertTrue(channel1.equals(channel1));
        assertTrue(!channel1.equals(new Object()));
        assertTrue(!channel1.equals(channel2));
        assertTrue(channel1.equals(channel3));
    }


    private Channel getChannel() {
        return new Channel() {
            private Map<String, Object> attributes = new HashMap<String, Object>();
            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public boolean isConnected() {
                return false;
            }

            @Override
            public boolean hasAttribute(String key) {
                return attributes.containsKey(key);
            }

            @Override
            public Object getAttribute(String key) {
                return attributes.get(key);
            }

            @Override
            public void setAttribute(String key, Object value) {
                attributes.put(key, value);
            }

            @Override
            public void removeAttribute(String key) {
                attributes.remove(key);
            }

            @Override
            public URL getUrl() {
                return null;
            }

            @Override
            public ChannelHandler getChannelHandler() {
                return null;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return new InetSocketAddress(8888);
            }

            @Override
            public void send(Object message) throws RemotingException {
            }

            @Override
            public void send(Object message, boolean sent) throws RemotingException {
                isSent = sent;
                inSend = true;
                if (message instanceof Response
                        || message instanceof String) {
                    isRequest = false;
                } else {
                    isRequest = true;
                }
            }

            @Override
            public void close() {
                isClosed = true;
            }

            @Override
            public void close(int timeout) {

            }

            @Override
            public void startClose() {
                closing = true;
            }

            @Override
            public boolean isClosed() {
                return false;
            }
        };
    }
}