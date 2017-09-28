/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * ChangeTelnetHandlerTest.java
 *
 * @author tony.chenl
 */
public class ChangeTelnetHandlerTest {

    private static TelnetHandler change = new ChangeTelnetHandler();
    private Channel mockChannel;
    private Invoker<DemoService> mockInvoker;

    @AfterClass
    public static void tearDown() {

    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockChannel = EasyMock.createMock(Channel.class);
        mockInvoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn("com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService").anyTimes();
        mockChannel.setAttribute("telnet.service", "DemoService");
        EasyMock.expectLastCall().anyTimes();
        mockChannel.setAttribute("telnet.service", "com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService");
        EasyMock.expectLastCall().anyTimes();
        mockChannel.setAttribute("telnet.service", "demo");
        EasyMock.expectLastCall().anyTimes();
        mockChannel.removeAttribute("telnet.service");
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(mockInvoker.getInterface()).andReturn(DemoService.class).anyTimes();
        EasyMock.expect(mockInvoker.getUrl()).andReturn(URL.valueOf("dubbo://127.0.0.1:20883/demo")).anyTimes();
        EasyMock.replay(mockChannel, mockInvoker);
    }

    @After
    public void after() {
        ProtocolUtils.closeAll();
        EasyMock.reset(mockChannel, mockInvoker);
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
        String result = change.telnet(mockChannel, "com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService");
        assertEquals("Used the com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService as default.\r\nYou can cancel default service by command: cd /",
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
        assertEquals("Cancelled default service com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService.", result);
    }

    @Test
    public void testChangeCancel2() throws RemotingException {
        String result = change.telnet(mockChannel, "/");
        assertEquals("Cancelled default service com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService.", result);
    }
}