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
package com.alibaba.dubbo.remoting.handler;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeHandler;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

//TODO response test
public class HeaderExchangeHandlerTest {

    @Test
    public void test_received_request_oneway() throws RemotingException {
        final Channel mchannel = new MockedChannel();

        final Person requestdata = new Person("charles");
        Request request = new Request();
        request.setTwoWay(false);
        request.setData(requestdata);

        ExchangeHandler exhandler = new MockedExchangeHandler() {
            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                Assert.assertEquals(requestdata, message);
            }
        };
        HeaderExchangeHandler hexhandler = new HeaderExchangeHandler(exhandler);
        hexhandler.received(mchannel, request);
    }

    @Test
    public void test_received_request_twoway() throws RemotingException {
        final Person requestdata = new Person("charles");
        final Request request = new Request();
        request.setTwoWay(true);
        request.setData(requestdata);

        final AtomicInteger count = new AtomicInteger(0);
        final Channel mchannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Response res = (Response) message;
                Assert.assertEquals(request.getId(), res.getId());
                Assert.assertEquals(request.getVersion(), res.getVersion());
                Assert.assertEquals(Response.OK, res.getStatus());
                Assert.assertEquals(requestdata, res.getResult());
                Assert.assertEquals(null, res.getErrorMessage());
                count.incrementAndGet();
            }
        };
        ExchangeHandler exhandler = new MockedExchangeHandler() {
            @Override
            public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
                return request;
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                Assert.fail();
            }
        };
        HeaderExchangeHandler hexhandler = new HeaderExchangeHandler(exhandler);
        hexhandler.received(mchannel, request);
        Assert.assertEquals(1, count.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_received_request_twoway_error_nullhandler() throws RemotingException {
        new HeaderExchangeHandler(null);
    }

    @Test
    public void test_received_request_twoway_error_reply() throws RemotingException {
        final Person requestdata = new Person("charles");
        final Request request = new Request();
        request.setTwoWay(true);
        request.setData(requestdata);

        final AtomicInteger count = new AtomicInteger(0);
        final Channel mchannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Response res = (Response) message;
                Assert.assertEquals(request.getId(), res.getId());
                Assert.assertEquals(request.getVersion(), res.getVersion());
                Assert.assertEquals(Response.SERVICE_ERROR, res.getStatus());
                Assert.assertNull(res.getResult());
                Assert.assertTrue(res.getErrorMessage().contains(BizException.class.getName()));
                count.incrementAndGet();
            }
        };
        ExchangeHandler exhandler = new MockedExchangeHandler() {
            @Override
            public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
                throw new BizException();
            }
        };
        HeaderExchangeHandler hexhandler = new HeaderExchangeHandler(exhandler);
        hexhandler.received(mchannel, request);
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void test_received_request_twoway_error_reqeustBroken() throws RemotingException {
        final Request request = new Request();
        request.setTwoWay(true);
        request.setData(new BizException());
        request.setBroken(true);

        final AtomicInteger count = new AtomicInteger(0);
        final Channel mchannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Response res = (Response) message;
                Assert.assertEquals(request.getId(), res.getId());
                Assert.assertEquals(request.getVersion(), res.getVersion());
                Assert.assertEquals(Response.BAD_REQUEST, res.getStatus());
                Assert.assertNull(res.getResult());
                Assert.assertTrue(res.getErrorMessage().contains(BizException.class.getName()));
                count.incrementAndGet();
            }
        };
        HeaderExchangeHandler hexhandler = new HeaderExchangeHandler(new MockedExchangeHandler());
        hexhandler.received(mchannel, request);
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void test_received_request_event_readonly() throws RemotingException {
        final Request request = new Request();
        request.setTwoWay(true);
        request.setEvent(Request.READONLY_EVENT);

        final Channel mchannel = new MockedChannel();
        HeaderExchangeHandler hexhandler = new HeaderExchangeHandler(new MockedExchangeHandler());
        hexhandler.received(mchannel, request);
        Assert.assertTrue(mchannel.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY));
    }

    @Test
    public void test_received_request_event_other_discard() throws RemotingException {
        final Request request = new Request();
        request.setTwoWay(true);
        request.setEvent("my event");

        final Channel mchannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Assert.fail();
            }
        };
        HeaderExchangeHandler hexhandler = new HeaderExchangeHandler(new MockedExchangeHandler() {

            @Override
            public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
                Assert.fail();
                throw new RemotingException(channel, "");
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                Assert.fail();
                throw new RemotingException(channel, "");
            }
        });
        hexhandler.received(mchannel, request);
    }

    private class BizException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private class MockedExchangeHandler extends MockedChannelHandler implements ExchangeHandler {

        public String telnet(Channel channel, String message) throws RemotingException {
            throw new UnsupportedOperationException();
        }

        public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
            throw new UnsupportedOperationException();
        }
    }

    private class Person {
        private String name;

        public Person(String name) {
            super();
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person [name=" + name + "]";
        }
    }
}
