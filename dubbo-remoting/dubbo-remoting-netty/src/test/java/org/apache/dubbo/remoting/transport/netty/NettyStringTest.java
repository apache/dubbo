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
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Date: 4/26/11
 * Time: 4:13 PM
 */
public class NettyStringTest {
    static ExchangeServer server;
    static ExchangeChannel client;

    @BeforeAll
    public static void setUp() throws Exception {
        //int port = (int) (1000 * Math.random() + 10000);
        //int port = 10001;
        int port = NetUtils.getAvailablePort();
        System.out.println(port);
        server = Exchangers.bind(URL.valueOf("telnet://0.0.0.0:" + port + "?server=netty3"), new TelnetServerHandler());
        client = Exchangers.connect(URL.valueOf("telnet://127.0.0.1:" + port + "?client=netty3"), new TelnetClientHandler());
    }

    @AfterAll
    public static void tearDown() throws Exception {
        try {
            if (server != null)
                server.close();
        } finally {
            if (client != null)
                client.close();
        }
    }

    @Test
    public void testHandler() throws Exception {
        //Thread.sleep(20000);
        /*client.request("world\r\n");
        Future future = client.request("world", 10000);
        String result = (String)future.get();
        Assertions.assertEquals("Did you say 'world'?\r\n",result);*/
    }
}