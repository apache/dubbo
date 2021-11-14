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
package org.apache.dubbo.rpc.protocol.dubbo.status;

import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.decode.MockChannel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * {@link ServerStatusChecker}
 */
public class ServerStatusCheckerTest {

    @Test
    public void test() {
        ServerStatusChecker serverStatusChecker = new ServerStatusChecker();
        Status status = serverStatusChecker.check();
        Assertions.assertEquals(status.getLevel(), Status.Level.UNKNOWN);

        DubboProtocol dubboProtocol = Mockito.mock(DubboProtocol.class);
        ProtocolServer protocolServer = Mockito.mock(ProtocolServer.class);
        RemotingServer remotingServer = Mockito.mock(RemotingServer.class);
        List<ProtocolServer> servers = Arrays.asList(protocolServer);
        Mockito.when(dubboProtocol.getServers()).thenReturn(servers);
        Mockito.when(protocolServer.getRemotingServer()).thenReturn(remotingServer);
        Mockito.when(remotingServer.isBound()).thenReturn(true);
        Mockito.when(remotingServer.getLocalAddress()).thenReturn(InetSocketAddress.createUnresolved("127.0.0.1", 9999));
        Mockito.when(remotingServer.getChannels()).thenReturn(Arrays.asList(new MockChannel()));


        try (MockedStatic<DubboProtocol> mockDubboProtocol = Mockito.mockStatic(DubboProtocol.class)) {
            mockDubboProtocol.when(() -> DubboProtocol.getDubboProtocol()).thenReturn(dubboProtocol);
            status = serverStatusChecker.check();
            Assertions.assertEquals(status.getLevel(), Status.Level.OK);
            // In JDK 17 : 127.0.0.1/<unresolved>:9999(clients:1)
            Assertions.assertTrue(status.getMessage().contains("127.0.0.1"));
            Assertions.assertTrue(status.getMessage().contains("9999(clients:1)"));

            Mockito.when(remotingServer.isBound()).thenReturn(false);
            status = serverStatusChecker.check();
            Assertions.assertEquals(status.getLevel(), Status.Level.ERROR);
            // In JDK 17 : 127.0.0.1/<unresolved>:9999
            Assertions.assertTrue(status.getMessage().contains("127.0.0.1"));
            Assertions.assertTrue(status.getMessage().contains("9999"));
        }
    }
}
