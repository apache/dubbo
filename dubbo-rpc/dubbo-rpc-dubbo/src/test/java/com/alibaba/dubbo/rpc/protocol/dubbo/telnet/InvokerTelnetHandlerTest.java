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
package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        mockInvoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(mockInvoker.getInterface()).andReturn(DemoService.class).anyTimes();
        EasyMock.expect(mockInvoker.getUrl()).andReturn(URL.valueOf("dubbo://127.0.0.1:20883/demo")).anyTimes();
        EasyMock.expect(mockInvoker.invoke((Invocation) EasyMock.anyObject())).andReturn(new RpcResult("ok")).anyTimes();
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn("com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService").anyTimes();
        EasyMock.expect(mockChannel.getLocalAddress()).andReturn(NetUtils.toAddress("127.0.0.1:5555")).anyTimes();
        EasyMock.expect(mockChannel.getRemoteAddress()).andReturn(NetUtils.toAddress("127.0.0.1:20883")).anyTimes();
        EasyMock.replay(mockChannel, mockInvoker);
        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = invoke.telnet(mockChannel, "DemoService.echo(\"ok\")");
        assertTrue(result.contains("Use default service com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService.\r\n\"ok\"\r\n"));
        EasyMock.reset(mockChannel, mockInvoker);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeAutoFindMethod() throws RemotingException {
        mockInvoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(mockInvoker.getInterface()).andReturn(DemoService.class).anyTimes();
        EasyMock.expect(mockInvoker.getUrl()).andReturn(URL.valueOf("dubbo://127.0.0.1:20883/demo")).anyTimes();
        EasyMock.expect(mockInvoker.invoke((Invocation) EasyMock.anyObject())).andReturn(new RpcResult("ok")).anyTimes();
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn(null).anyTimes();
        EasyMock.expect(mockChannel.getLocalAddress()).andReturn(NetUtils.toAddress("127.0.0.1:5555")).anyTimes();
        EasyMock.expect(mockChannel.getRemoteAddress()).andReturn(NetUtils.toAddress("127.0.0.1:20883")).anyTimes();
        EasyMock.replay(mockChannel, mockInvoker);
        DubboProtocol.getDubboProtocol().export(mockInvoker);
        String result = invoke.telnet(mockChannel, "echo(\"ok\")");
        assertTrue(result.contains("ok"));
        EasyMock.reset(mockChannel, mockInvoker);
    }

    @Test
    public void testMessageNull() throws RemotingException {
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn(null).anyTimes();
        EasyMock.replay(mockChannel);
        String result = invoke.telnet(mockChannel, null);
        assertEquals("Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})",
                result);
        EasyMock.reset(mockChannel);
    }

    @Test
    public void testInvaildMessage() throws RemotingException {
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn(null).anyTimes();
        EasyMock.replay(mockChannel);
        String result = invoke.telnet(mockChannel, "(");
        assertEquals("Invalid parameters, format: service.method(args)", result);
        EasyMock.reset(mockChannel);
    }
}