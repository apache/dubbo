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

import org.apache.dubbo.common.utils.DubboAppender;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.Server;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Client reconnect test
 */
public class ClientReconnectTest {

    @BeforeEach
    public void clear() {
        DubboAppender.clear();
    }

    @Test
    public void testReconnect() throws RemotingException, InterruptedException {
        {
            int port = NetUtils.getAvailablePort();
            Client client = startClient(port, 200);
            Assertions.assertFalse(client.isConnected());
            Server server = startServer(port);
            for (int i = 0; i < 100 && !client.isConnected(); i++) {
                Thread.sleep(10);
            }
            Assertions.assertTrue(client.isConnected());
            client.close(2000);
            server.close(2000);
        }
        {
            int port = NetUtils.getAvailablePort();
            Client client = startClient(port, 20000);
            Assertions.assertFalse(client.isConnected());
            Server server = startServer(port);
            for (int i = 0; i < 5; i++) {
                Thread.sleep(200);
            }
            Assertions.assertFalse(client.isConnected());
            client.close(2000);
            server.close(2000);
        }
    }


    public Client startClient(int port, int heartbeat) throws RemotingException {
        final String url = "exchange://127.0.0.1:" + port + "/client.reconnect.test?check=false&client=netty3&" + Constants.HEARTBEAT_KEY + "=" + heartbeat;
        return Exchangers.connect(url);
    }

    public Server startServer(int port) throws RemotingException {
        final String url = "exchange://127.0.0.1:" + port + "/client.reconnect.test?server=netty3";
        return Exchangers.bind(url, new HandlerAdapter());
    }

    static class HandlerAdapter extends ExchangeHandlerAdapter {
        @Override
        public void connected(Channel channel) throws RemotingException {
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
        }
    }
}
