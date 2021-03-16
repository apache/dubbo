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
package org.apache.dubbo.remoting.transport.netty;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ThreadNameTest {

    private NettyServer server;
    private NettyClient client;

    private URL serverURL;
    private URL clientURL;

    private ThreadNameVerifyHandler serverHandler;
    private ThreadNameVerifyHandler clientHandler;

    private static String serverRegex = "DubboServerHandler\\-localhost:(\\d+)\\-thread\\-(\\d+)";
    private static String clientRegex = "DubboClientHandler\\-thread\\-(\\d+)";

    @BeforeEach
    public void before() throws Exception {
        int port = NetUtils.getAvailablePort();
        serverURL = URL.valueOf("telnet://localhost?side=provider").setPort(port);
        clientURL = URL.valueOf("telnet://localhost?side=consumer").setPort(port);

        serverHandler = new ThreadNameVerifyHandler(serverRegex, false);
        clientHandler = new ThreadNameVerifyHandler(clientRegex, true);

        server = new NettyServer(serverURL, serverHandler);
        client = new NettyClient(clientURL, clientHandler);
    }

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
    public void testThreadName() throws Exception {
        client.send("hello");
        Thread.sleep(1000L * 5L);
        if (!serverHandler.isSuccess() || !clientHandler.isSuccess()) {
            Assertions.fail();
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
                success = Thread.currentThread().getName().matches(message);
            }
        }

        private void output(String method) {
            System.out.println(Thread.currentThread().getName()
                    + " " + (client ? "client " + method : "server " + method));
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            output("connected");
            checkThreadName();
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            output("disconnected");
            checkThreadName();
        }

        @Override
        public void sent(Channel channel, Object message) throws RemotingException {
            output("sent");
            checkThreadName();
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            output("received");
            checkThreadName();
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            output("caught");
            checkThreadName();
        }
    }

}
