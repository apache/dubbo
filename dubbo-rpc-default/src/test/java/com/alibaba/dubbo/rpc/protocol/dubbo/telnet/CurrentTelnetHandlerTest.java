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

import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;

/**
 * CountTelnetHandlerTest.java
 * 
 * @author tony.chenl
 */
public class CurrentTelnetHandlerTest {

    private static TelnetHandler count = new CurrentTelnetHandler();
    private Channel              mockChannel;

    @After
    public void after() {
        EasyMock.reset(mockChannel);
    }

    @Test
    public void testService() throws RemotingException {
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn("com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService").anyTimes();
        EasyMock.replay(mockChannel);
        String result = count.telnet(mockChannel, "");
        assertEquals("com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService", result);
    }

    @Test
    public void testSlash() throws RemotingException {
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn(null).anyTimes();
        EasyMock.replay(mockChannel);
        String result = count.telnet(mockChannel, "");
        assertEquals("/", result);
    }
    
    @Test
    public void testMessageError() throws RemotingException {
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.expect(mockChannel.getAttribute("telnet.service")).andReturn(null).anyTimes();
        EasyMock.replay(mockChannel);
        String result = count.telnet(mockChannel, "test");
        assertEquals("Unsupported parameter test for pwd.", result);
    }
}