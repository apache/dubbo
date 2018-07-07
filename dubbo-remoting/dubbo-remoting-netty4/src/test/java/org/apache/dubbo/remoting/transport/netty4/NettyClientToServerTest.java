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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.Replier;

/**
 * Netty4ClientToServerTest
 */
public class NettyClientToServerTest extends ClientToServerTest {

    protected ExchangeServer newServer(int port, Replier<?> receiver) throws RemotingException {
        return Exchangers.bind(URL.valueOf("exchange://localhost:" + port + "?server=netty4"), receiver);
    }

    protected ExchangeChannel newClient(int port) throws RemotingException {
        return Exchangers.connect(URL.valueOf("exchange://localhost:" + port + "?client=netty4"));
    }

}