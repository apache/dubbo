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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.transport.dispatcher.FakeChannelHandlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

public class HeartbeatHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandlerTest.class);

    private ExchangeServer server;
    private ExchangeClient client;

    @AfterEach
    public void after() throws Exception {
        if (client != null) {
            client.close();
            client = null;
        }

        if (server != null) {
            server.close();
            server = null;
        }
    }

    @Test
    public void testServerHeartbeat() throws Exception {
        URL serverURL = URL.valueOf("header://localhost:55555?transporter=netty3");
        serverURL = serverURL.addParameter(Constants.HEARTBEAT_KEY, 1000);
        TestHeartbeatHandler handler = new TestHeartbeatHandler();
        server = Exchangers.bind(serverURL, handler);
        System.out.println("Server bind successfully");

        FakeChannelHandlers.setTestingChannelHandlers();
        serverURL = serverURL.removeParameter(Constants.HEARTBEAT_KEY);

        // Let the client not reply to the heartbeat, and turn off automatic reconnect to simulate the client dropped.
        serverURL = serverURL.addParameter(Constants.HEARTBEAT_KEY, 600 * 1000);
        serverURL = serverURL.addParameter(Constants.RECONNECT_KEY, false);

        client = Exchangers.connect(serverURL);
        Thread.sleep(10000);
        Assertions.assertTrue(handler.disconnectCount > 0);
        System.out.println("disconnect count " + handler.disconnectCount);
    }

    @Test
    public void testHeartbeat() throws Exception {
        URL serverURL = URL.valueOf("header://localhost:55555?transporter=netty3");
        serverURL = serverURL.addParameter(Constants.HEARTBEAT_KEY, 1000);
        TestHeartbeatHandler handler = new TestHeartbeatHandler();
        server = Exchangers.bind(serverURL, handler);
        System.out.println("Server bind successfully");

        client = Exchangers.connect(serverURL);
        Thread.sleep(10000);
        System.err.println("++++++++++++++ disconnect count " + handler.disconnectCount);
        System.err.println("++++++++++++++ connect count " + handler.connectCount);
        Assertions.assertTrue(handler.disconnectCount == 0);
        Assertions.assertTrue(handler.connectCount == 1);
    }

    @Test
    public void testClientHeartbeat() throws Exception {
        FakeChannelHandlers.setTestingChannelHandlers();
        URL serverURL = URL.valueOf("header://localhost:55555?transporter=netty3");
        TestHeartbeatHandler handler = new TestHeartbeatHandler();
        server = Exchangers.bind(serverURL, handler);
        System.out.println("Server bind successfully");

        FakeChannelHandlers.resetChannelHandlers();
        serverURL = serverURL.addParameter(Constants.HEARTBEAT_KEY, 1000);
        client = Exchangers.connect(serverURL);
        Thread.sleep(10000);
        Assertions.assertTrue(handler.connectCount > 0);
        System.out.println("connect count " + handler.connectCount);
    }

    class TestHeartbeatHandler implements ExchangeHandler {

        public int disconnectCount = 0;
        public int connectCount = 0;

        public CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException {
            return CompletableFuture.completedFuture(request);
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            ++connectCount;
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            ++disconnectCount;
        }

        @Override
        public void sent(Channel channel, Object message) throws RemotingException {

        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            logger.error(this.getClass().getSimpleName() + message.toString());
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            exception.printStackTrace();
        }

        public String telnet(Channel channel, String message) throws RemotingException {
            return message;
        }
    }

}
