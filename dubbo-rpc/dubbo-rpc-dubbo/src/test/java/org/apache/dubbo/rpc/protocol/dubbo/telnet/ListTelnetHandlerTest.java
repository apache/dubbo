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
import org.apache.dubbo.common.utils.ReflectUtils;
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
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * CountTelnetHandlerTest.java
 */
public class ListTelnetHandlerTest {

    private static TelnetHandler list = new ListTelnetHandler();
    private static String detailMethods;
    private static String methodsName;
    private Channel mockChannel;
    private Invoker<DemoService> mockInvoker;

    @BeforeClass
    public static void setUp() {
        StringBuilder buf = new StringBuilder();
        StringBuilder buf2 = new StringBuilder();
        Method[] methods = DemoService.class.getMethods();
        for (Method method : methods) {
            if (buf.length() > 0) {
                buf.append("\r\n");
            }
            if (buf2.length() > 0) {
                buf2.append("\r\n");
            }
            buf2.append(method.getName());
            buf.append(ReflectUtils.getName(method));
        }
        detailMethods = buf.toString();
        methodsName = buf2.toString();

        ProtocolUtils.closeAll();
    }

    @After
    public void after() {
        ProtocolUtils.closeAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListDetailService() throws RemotingException {
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20885/demo"));
        given(mockInvoker.invoke(any(Invocation.class))).willReturn(new RpcResult("ok"));
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");

        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = list.telnet(mockChannel, "-l DemoService");
        assertEquals(detailMethods, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListService() throws RemotingException {
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20885/demo"));
        given(mockInvoker.invoke(any(Invocation.class))).willReturn(new RpcResult("ok"));
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");

        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = list.telnet(mockChannel, "DemoService");
        assertEquals(methodsName, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testList() throws RemotingException {
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20885/demo"));
        given(mockInvoker.invoke(any(Invocation.class))).willReturn(new RpcResult("ok"));
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);

        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = list.telnet(mockChannel, "");
        assertEquals("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListDetail() throws RemotingException {
        int port = NetUtils.getAvailablePort();
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:" + port + "/demo"));
        given(mockInvoker.invoke(any(Invocation.class))).willReturn(new RpcResult("ok"));
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);

        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = list.telnet(mockChannel, "-l");
        assertEquals("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService -> dubbo://127.0.0.1:" + port + "/demo", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListDefault() throws RemotingException {
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20885/demo"));
        given(mockInvoker.invoke(any(Invocation.class))).willReturn(new RpcResult("ok"));
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");

        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = list.telnet(mockChannel, "");
        assertEquals("Use default service org.apache.dubbo.rpc.protocol.dubbo.support.DemoService.\r\n\r\n"
                + methodsName, result);
    }

    @Test
    public void testInvaildMessage() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);

        String result = list.telnet(mockChannel, "xx");
        assertEquals("No such service xx", result);
    }
}