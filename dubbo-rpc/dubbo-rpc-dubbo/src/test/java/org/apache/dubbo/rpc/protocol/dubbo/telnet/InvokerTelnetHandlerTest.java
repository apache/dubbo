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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * CountTelnetHandlerTest.java
 */
public class InvokerTelnetHandlerTest {

    private static TelnetHandler invoke = new InvokeTelnetHandler();
    private Channel mockChannel;
    private Invoker<DemoService> mockInvoker;

    @After
    public void after() {
        ProtocolUtils.closeAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeDefaultSService() throws RemotingException {
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20883/demo"));
        given(mockInvoker.invoke(any(Invocation.class))).willReturn(new RpcResult("ok"));
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20883"));

        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = invoke.telnet(mockChannel, "DemoService.echo(\"ok\")");
        assertTrue(result.contains("Use default service org.apache.dubbo.rpc.protocol.dubbo.support.DemoService.\r\n\"ok\"\r\n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeAutoFindMethod() throws RemotingException {
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20883/demo"));
        given(mockInvoker.invoke(any(Invocation.class))).willReturn(new RpcResult("ok"));
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20883"));

        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = invoke.telnet(mockChannel, "echo(\"ok\")");
        assertTrue(result.contains("ok"));
    }

    @Test
    public void testMessageNull() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);

        String result = invoke.telnet(mockChannel, null);
        assertEquals("Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})",
                result);
    }

    @Test
    public void testInvaildMessage() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);

        String result = invoke.telnet(mockChannel, "(");
        assertEquals("Invalid parameters, format: service.method(args)", result);
    }
}