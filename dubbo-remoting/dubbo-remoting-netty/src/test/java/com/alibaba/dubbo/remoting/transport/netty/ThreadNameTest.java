/*
 * Copyright 1999-2012 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport.netty;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ThreadNameTest {

    private NettyServer server;
    private NettyClient client;

    private URL serverURL;
    private URL clientURL;

    private ThreadNameVerifyHandler serverHandler;
    private ThreadNameVerifyHandler clientHandler;

    @Before
    public void before() throws Exception {
        int port = 55555;
        serverURL = URL.valueOf("netty://localhost").setPort(port);
        clientURL = URL.valueOf("netty://localhost").setPort(port);

        serverHandler = new ThreadNameVerifyHandler(String.valueOf(port), false);
        clientHandler = new ThreadNameVerifyHandler(String.valueOf(port), true);

        server = new NettyServer(serverURL, serverHandler);
        client = new NettyClient(clientURL, clientHandler);
    }

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
    public void testThreadName() throws Exception {
        client.send("hello");
        Thread.sleep(1000L * 5L);
        if (!serverHandler.isSuccess() || !clientHandler.isSuccess()) {
            Assert.fail();
        }
    }

    class ThreadNameVerifyHandler implements ChannelHandler {

        private String message;
        private boolean success;
        private boolean client;

        ThreadNameVerifyHandler(String msg, boolean client) {
            message = msg;
            this.client = client;
        }

        public boolean isSuccess() {
            return success;
        }

        private void checkThreadName() {
            if (!success) {
                success = Thread.currentThread().getName().contains(message);
            }
        }

        private void output(String method) {
            System.out.println(Thread.currentThread().getName()
                    + " " + (client ? "client " + method : "server " + method));
        }

        public void connected(Channel channel) throws RemotingException {
            output("connected");
            checkThreadName();
        }

        public void disconnected(Channel channel) throws RemotingException {
            output("disconnected");
            checkThreadName();
        }

        public void sent(Channel channel, Object message) throws RemotingException {
            output("sent");
            checkThreadName();
        }

        public void received(Channel channel, Object message) throws RemotingException {
            output("received");
            checkThreadName();
        }

        public void caught(Channel channel, Throwable exception) throws RemotingException {
            output("caught");
            checkThreadName();
        }
    }

}