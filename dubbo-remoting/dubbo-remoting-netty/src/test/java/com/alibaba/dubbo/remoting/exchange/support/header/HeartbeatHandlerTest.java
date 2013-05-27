/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.remoting.exchange.support.header;

import org.junit.After;
import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.transport.dispatcher.FakeChannelHandlers;

import junit.framework.Assert;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class HeartbeatHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandlerTest.class);

    private ExchangeServer server;
    private ExchangeClient client;

    @After
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
        URL serverURL = URL.valueOf("header://localhost:55555");
        serverURL = serverURL.addParameter(Constants.HEARTBEAT_KEY, 1000);
        TestHeartbeatHandler handler = new TestHeartbeatHandler();
        server = Exchangers.bind(serverURL, handler);
        System.out.println("Server bind successfully");

        FakeChannelHandlers.setTestingChannelHandlers();
        serverURL = serverURL.removeParameter(Constants.HEARTBEAT_KEY);
        client = Exchangers.connect(serverURL);
        Thread.sleep(10000);
        Assert.assertTrue(handler.disconnectCount > 0);
        System.out.println("disconnect count " + handler.disconnectCount);
    }

    @Test
    public void testHeartbeat() throws Exception {
        URL serverURL = URL.valueOf("header://localhost:55555");
        serverURL = serverURL.addParameter(Constants.HEARTBEAT_KEY, 1000);
        TestHeartbeatHandler handler = new TestHeartbeatHandler();
        server = Exchangers.bind(serverURL, handler);
        System.out.println("Server bind successfully");

        client = Exchangers.connect(serverURL);
        Thread.sleep(10000);
        System.err.println("++++++++++++++ disconnect count " + handler.disconnectCount);
        System.err.println("++++++++++++++ connect count " + handler.connectCount);
        Assert.assertTrue(handler.disconnectCount == 0);
        Assert.assertTrue(handler.connectCount == 1);
    }

    @Test
    public void testClientHeartbeat() throws Exception {
        FakeChannelHandlers.setTestingChannelHandlers();
        URL serverURL = URL.valueOf("header://localhost:55555");
        TestHeartbeatHandler handler = new TestHeartbeatHandler();
        server = Exchangers.bind(serverURL, handler);
        System.out.println("Server bind successfully");

        FakeChannelHandlers.resetChannelHandlers();
        serverURL = serverURL.addParameter(Constants.HEARTBEAT_KEY, 1000);
        client = Exchangers.connect(serverURL);
        Thread.sleep(10000);
        Assert.assertTrue(handler.connectCount > 0);
        System.out.println("connect count " + handler.connectCount);
    }

    class TestHeartbeatHandler implements ExchangeHandler {

        public int disconnectCount = 0;
        public int connectCount = 0;

        public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
            return request;
        }

        public void connected(Channel channel) throws RemotingException {
            ++connectCount;
        }

        public void disconnected(Channel channel) throws RemotingException {
            ++disconnectCount;
        }

        public void sent(Channel channel, Object message) throws RemotingException {

        }

        public void received(Channel channel, Object message) throws RemotingException {
        	logger.error(this.getClass().getSimpleName() + message.toString());
        }

        public void caught(Channel channel, Throwable exception) throws RemotingException {
            exception.printStackTrace();
        }

        public String telnet(Channel channel, String message) throws RemotingException {
            return message;
        }
    }

}
