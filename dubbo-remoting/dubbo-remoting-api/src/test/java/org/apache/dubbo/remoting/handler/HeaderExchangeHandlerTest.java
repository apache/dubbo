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
package org.apache.dubbo.remoting.handler;


import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.remoting.exchange.support.header.HeaderExchangeHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.HEARTBEAT_EVENT;
import static org.apache.dubbo.common.constants.CommonConstants.READONLY_EVENT;

//TODO response test
class HeaderExchangeHandlerTest {

    @Test
    void testReceivedRequestOneway() throws RemotingException {
        final Channel mockChannel = new MockedChannel();

        final Person requestData = new Person("charles");
        Request request = new Request();
        request.setTwoWay(false);
        request.setData(requestData);

        ExchangeHandler exHandler = new MockedExchangeHandler() {
            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                Assertions.assertEquals(requestData, message);
            }
        };
        HeaderExchangeHandler headExHandler = new HeaderExchangeHandler(exHandler);
        headExHandler.received(mockChannel, request);
    }

    @Test
    void testReceivedRequestTwoway() throws RemotingException {
        final Person requestData = new Person("charles");
        final Request request = new Request();
        request.setTwoWay(true);
        request.setData(requestData);

        final AtomicInteger count = new AtomicInteger(0);
        final Channel mockChannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Response res = (Response) message;
                Assertions.assertEquals(request.getId(), res.getId());
                Assertions.assertEquals(request.getVersion(), res.getVersion());
                Assertions.assertEquals(Response.OK, res.getStatus());
                Assertions.assertEquals(requestData, res.getResult());
                Assertions.assertNull(res.getErrorMessage());
                count.incrementAndGet();
            }
        };
        ExchangeHandler exHandler = new MockedExchangeHandler() {
            @Override
            public CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException {
                return CompletableFuture.completedFuture(request);
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                Assertions.fail();
            }
        };
        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(exHandler);
        headerExchangeHandler.received(mockChannel, request);
        Assertions.assertEquals(1, count.get());
    }

    @Test
    void testReceivedRequestTwowayErrorWithNullHandler() throws RemotingException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HeaderExchangeHandler(null));
    }

    @Test
    void testReceivedRequestTwowayErrorReply() throws RemotingException {
        final Person requestData = new Person("charles");
        final Request request = new Request();
        request.setTwoWay(true);
        request.setData(requestData);

        final AtomicInteger count = new AtomicInteger(0);
        final Channel mockChannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Response res = (Response) message;
                Assertions.assertEquals(request.getId(), res.getId());
                Assertions.assertEquals(request.getVersion(), res.getVersion());
                Assertions.assertEquals(Response.SERVICE_ERROR, res.getStatus());
                Assertions.assertNull(res.getResult());
                Assertions.assertTrue(res.getErrorMessage().contains(BizException.class.getName()));
                count.incrementAndGet();
            }
        };
        ExchangeHandler exHandler = new MockedExchangeHandler() {
            @Override
            public CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException {
                throw new BizException();
            }
        };
        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(exHandler);
        headerExchangeHandler.received(mockChannel, request);
        Assertions.assertEquals(1, count.get());
    }

    @Test
    void testReceivedRequestTwowayErrorRequestBroken() throws RemotingException {
        final Request request = new Request();
        request.setTwoWay(true);
        request.setData(new BizException());
        request.setBroken(true);

        final AtomicInteger count = new AtomicInteger(0);
        final Channel mockChannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Response res = (Response) message;
                Assertions.assertEquals(request.getId(), res.getId());
                Assertions.assertEquals(request.getVersion(), res.getVersion());
                Assertions.assertEquals(Response.BAD_REQUEST, res.getStatus());
                Assertions.assertNull(res.getResult());
                Assertions.assertTrue(res.getErrorMessage().contains(BizException.class.getName()));
                count.incrementAndGet();
            }
        };
        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(new MockedExchangeHandler());
        headerExchangeHandler.received(mockChannel, request);
        Assertions.assertEquals(1, count.get());
    }

    @Test
    void testReceivedRequestEventReadonly() throws RemotingException {
        final Request request = new Request();
        request.setTwoWay(true);
        request.setEvent(READONLY_EVENT);

        final Channel mockChannel = new MockedChannel();
        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(new MockedExchangeHandler());
        headerExchangeHandler.received(mockChannel, request);
        Assertions.assertTrue(mockChannel.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY));
    }

    @Test
    void testReceivedRequestEventOtherDiscard() throws RemotingException {
        final Request request = new Request();
        request.setTwoWay(true);
        request.setEvent("my event");

        final Channel mockChannel = new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Assertions.fail();
            }
        };
        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(new MockedExchangeHandler() {

            @Override
            public CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException {
                Assertions.fail();
                throw new RemotingException(channel, "");
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                Assertions.fail();
                throw new RemotingException(channel, "");
            }
        });
        headerExchangeHandler.received(mockChannel, request);
    }

    @Test
    void testReceivedResponseHeartbeatEvent() throws Exception {
        Channel mockChannel = new MockedChannel();
        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(new MockedExchangeHandler());
        Response response = new Response(1);
        response.setStatus(Response.OK);
        response.setEvent(true);
        response.setResult(HEARTBEAT_EVENT);
        headerExchangeHandler.received(mockChannel, response);
    }

    @Test
    void testReceivedResponse() throws Exception {
        Request request = new Request(1);
        request.setTwoWay(true);
        Channel mockChannel = new MockedChannel();
        DefaultFuture future = DefaultFuture.newFuture(mockChannel, request, 5000, null);

        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(new MockedExchangeHandler());
        Response response = new Response(1);
        response.setStatus(Response.OK);
        response.setResult("MOCK_DATA");
        headerExchangeHandler.received(mockChannel, response);

        Object result = future.get();
        Assertions.assertEquals(result.toString(),"MOCK_DATA");
    }

    private class BizException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private class MockedExchangeHandler extends MockedChannelHandler implements ExchangeHandler {

        public String telnet(Channel channel, String message) throws RemotingException {
            throw new UnsupportedOperationException();
        }

        public CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException {
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
