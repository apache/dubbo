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
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.Exchangers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 5/3/11
 * Time: 5:47 PM
 */
public class NettyClientTest {
    static RemotingServer server;
    static int port = NetUtils.getAvailablePort();

    @BeforeAll
    public static void setUp() throws Exception {
        server = Exchangers.bind(URL.valueOf("exchange://localhost:" + port + "?server=netty3"), new TelnetServerHandler());
    }

    @AfterAll
    public static void tearDown() {
        try {
            if (server != null)
                server.close();
        } finally {
        }
    }

    public static void main(String[] args) throws RemotingException, InterruptedException {
        ExchangeChannel client = Exchangers.connect(URL.valueOf("exchange://10.20.153.10:20880?client=netty3&heartbeat=1000"));
        Thread.sleep(60 * 1000 * 50);
    }

    @Test
    public void testClientClose() throws Exception {
        List<ExchangeChannel> clients = new ArrayList<ExchangeChannel>(100);
        for (int i = 0; i < 100; i++) {
            ExchangeChannel client = Exchangers.connect(URL.valueOf("exchange://localhost:" + port + "?client=netty3"));
            Thread.sleep(5);
            clients.add(client);
        }
        for (ExchangeChannel client : clients) {
            client.close();
        }
        Thread.sleep(1000);
    }

    @Test
    public void testServerClose() throws Exception {
        for (int i = 0; i < 100; i++) {
            RemotingServer aServer = Exchangers.bind(URL.valueOf("exchange://localhost:" + NetUtils.getAvailablePort(6000) + "?server=netty3"), new TelnetServerHandler());
            aServer.close();
        }
    }
}