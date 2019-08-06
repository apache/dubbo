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
package org.apache.dubbo.rpc.protocol.dubbo.telnet;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * ChangeTelnetHandlerTest.java
 */
public class ChangeTelnetHandlerTest {

    private static TelnetHandler change = new ChangeTelnetHandler();
    private Channel mockChannel;
    private Invoker<DemoService> mockInvoker;

    @AfterAll
    public static void tearDown() {

    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() {
        mockChannel = mock(Channel.class);
        mockInvoker = mock(Invoker.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");
        mockChannel.setAttribute("telnet.service", "DemoService");
        givenLastCall();
        mockChannel.setAttribute("telnet.service", "org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");
        givenLastCall();
        mockChannel.setAttribute("telnet.service", "demo");
        givenLastCall();
        mockChannel.removeAttribute("telnet.service");
        givenLastCall();
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20884/demo"));
    }

    private void givenLastCall() {

    }

    @AfterEach
    public void after() {
        ProtocolUtils.closeAll();
        reset(mockChannel, mockInvoker);
    }

    @Test
    public void testChangeSimpleName() throws RemotingException {
        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = change.telnet(mockChannel, "DemoService");
        assertEquals("Used the DemoService as default.\r\nYou can cancel default service by command: cd /", result);
    }

    @Test
    public void testChangeName() throws RemotingException {
        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = change.telnet(mockChannel, "org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");
        assertEquals("Used the org.apache.dubbo.rpc.protocol.dubbo.support.DemoService as default.\r\nYou can cancel default service by command: cd /",
                result);
    }

    @Test
    public void testChangePath() throws RemotingException {
        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = change.telnet(mockChannel, "demo");
        assertEquals("Used the demo as default.\r\nYou can cancel default service by command: cd /", result);
    }

    @Test
    public void testChangeMessageNull() throws RemotingException {
        String result = change.telnet(mockChannel, null);
        assertEquals("Please input service name, eg: \r\ncd XxxService\r\ncd com.xxx.XxxService", result);
    }

    @Test
    public void testChangeServiceNotExport() throws RemotingException {
        String result = change.telnet(mockChannel, "demo");
        assertEquals("No such service demo", result);
    }

    @Test
    public void testChangeCancel() throws RemotingException {
        String result = change.telnet(mockChannel, "..");
        assertEquals("Cancelled default service org.apache.dubbo.rpc.protocol.dubbo.support.DemoService.", result);
    }

    @Test
    public void testChangeCancel2() throws RemotingException {
        String result = change.telnet(mockChannel, "/");
        assertEquals("Cancelled default service org.apache.dubbo.rpc.protocol.dubbo.support.DemoService.", result);
    }
}