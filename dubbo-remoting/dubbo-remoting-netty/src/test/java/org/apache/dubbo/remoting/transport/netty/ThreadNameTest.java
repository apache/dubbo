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
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ThreadNameTest {

    private NettyServer server;
    private NettyClient client;

    private URL serverURL;
    private URL clientURL;

    private ThreadNameVerifyHandler serverHandler;
    private ThreadNameVerifyHandler clientHandler;

    private static String serverRegex = "DubboServerHandler\\-localhost:(\\d+)\\-thread\\-(\\d+)";
    private static String clientRegex = "DubboClientHandler\\-thread\\-(\\d+)";

    private final CountDownLatch serverLatch = new CountDownLatch(1);
    private final CountDownLatch clientLatch = new CountDownLatch(1);

    @BeforeEach
    public void before() throws Exception {
        int port = NetUtils.getAvailablePort(20880 + new Random().nextInt(10000));
        serverURL = URL.valueOf("telnet://localhost?side=provider&codec=telnet")
            .setPort(port)
            .setScopeModel(ApplicationModel.defaultModel());
        clientURL = URL.valueOf("telnet://localhost?side=consumer&codec=telnet")
            .setPort(port)
            .setScopeModel(ApplicationModel.defaultModel());

        serverHandler = new ThreadNameVerifyHandler(serverRegex, false, serverLatch);
        clientHandler = new ThreadNameVerifyHandler(clientRegex, true, clientLatch);
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
        serverLatch.await(30, TimeUnit.SECONDS);
        clientLatch.await(30, TimeUnit.SECONDS);
        if (!serverHandler.isSuccess() || !clientHandler.isSuccess()) {
            Assertions.fail();
        }
    }

    class ThreadNameVerifyHandler implements ChannelHandler {

        private String message;
        private boolean success;
        private boolean client;
        private CountDownLatch latch;

        ThreadNameVerifyHandler(String msg, boolean client, CountDownLatch latch) {
            message = msg;
            this.client = client;
            this.latch = latch;
        }

        public boolean isSuccess() {
            return success;
        }

        private void checkThreadName() {
            if (!success) {
                success = Thread.currentThread().getName().matches(message);
            }
            if(success) {
                latch.countDown();
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
            // client: DubboClientHandler thread, server: DubboServerHandler or DubboSharedHandler thread.
            output("disconnected");
        }

        @Override
        public void sent(Channel channel, Object message) throws RemotingException {
            // main thread.
            output("sent");
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            // server: DubboServerHandler or DubboSharedHandler thread. 
            output("received");
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            // client: DubboClientHandler thread, server: ?
            output("caught");
        }
    }

}
