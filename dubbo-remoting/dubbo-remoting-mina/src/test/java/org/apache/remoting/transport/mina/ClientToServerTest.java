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
package org.apache.remoting.transport.mina;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.support.Replier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/**
 * ClientToServer
 */
public abstract class ClientToServerTest {

    protected static final String LOCALHOST = "127.0.0.1";

    protected ExchangeServer server;

    protected ExchangeChannel client;

    protected WorldHandler handler = new WorldHandler();

    protected abstract ExchangeServer newServer(int port, Replier<?> receiver) throws RemotingException;

    protected abstract ExchangeChannel newClient(int port) throws RemotingException;

    @BeforeEach
    protected void setUp() throws Exception {
        int port = NetUtils.getAvailablePort();
        server = newServer(port, handler);
        client = newClient(port);
    }

    @AfterEach
    protected void tearDown() {
        try {
            if (server != null)
                server.close();
        } finally {
            if (client != null)
                client.close();
        }
    }

    @Test
    public void testFuture() throws Exception {
        CompletableFuture<Object> future = client.request(new World("world"));
        Hello result = (Hello) future.get();
        Assertions.assertEquals("hello,world", result.getName());
    }

//    @Test
//    public void testCallback() throws Exception {
//        final Object waitter = new Object();
//        client.invoke(new World("world"), new InvokeCallback<Hello>() {
//            public void callback(Hello result) {
//                Assertions.assertEquals("hello,world", result.getName());
//                synchronized (waitter) {
//                    waitter.notifyAll();
//                }
//            }
//            public void onException(Throwable exception) {
//            }
//        });
//        synchronized (waitter) {
//            waitter.wait();
//        }
//    }

}