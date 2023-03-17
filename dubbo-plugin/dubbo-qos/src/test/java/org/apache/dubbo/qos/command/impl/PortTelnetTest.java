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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.legacy.service.DemoService;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

class PortTelnetTest {
    private BaseCommand port;

    private Invoker<DemoService> mockInvoker;
    private CommandContext mockCommandContext;

    private static final int availablePort = NetUtils.getAvailablePort();

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void before() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        port = new PortTelnet(frameworkModel);
        mockCommandContext = mock(CommandContext.class);
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:" + availablePort + "/demo"));

        frameworkModel.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME).export(mockInvoker);
    }

    @AfterEach
    public void afterEach() {
        FrameworkModel.destroyAll();
        reset(mockInvoker, mockCommandContext);
    }

    /**
     * In NAT network scenario, server's channel.getRemoteAddress() possibly get the address of network gateway, or
     * the address converted by NAT. In this case, check port only.
     */
    @Test
    void testListClient() throws Exception {
        ExchangeClient client1 = Exchangers.connect("dubbo://127.0.0.1:" + availablePort + "/demo");
        ExchangeClient client2 = Exchangers.connect("dubbo://127.0.0.1:" + availablePort + "/demo");
        Thread.sleep(100);
        String result = port.execute(mockCommandContext, new String[]{"-l", availablePort + ""});
        String client1Addr = client1.getLocalAddress().toString();
        String client2Addr = client2.getLocalAddress().toString();
        System.out.printf("Result: %s %n", result);
        System.out.printf("Client 1 Address %s %n", client1Addr);
        System.out.printf("Client 2 Address %s %n", client2Addr);
        assertTrue(result.contains(String.valueOf(client1.getLocalAddress().getPort())));
        assertTrue(result.contains(String.valueOf(client2.getLocalAddress().getPort())));
    }

    @Test
    void testListDetail() throws RemotingException {
        String result = port.execute(mockCommandContext, new String[]{"-l"});
        assertEquals("dubbo://127.0.0.1:" + availablePort + "", result);
    }

    @Test
    void testListAllPort() throws RemotingException {
        String result = port.execute(mockCommandContext, new String[0]);
        assertEquals("" + availablePort + "", result);
    }

    @Test
    void testErrorMessage() throws RemotingException {
        String result = port.execute(mockCommandContext, new String[]{"a"});
        assertEquals("Illegal port a, must be integer.", result);
    }

    @Test
    void testNoPort() throws RemotingException {
        String result = port.execute(mockCommandContext, new String[]{"-l", "20880"});
        assertEquals("No such port 20880", result);
    }
}
