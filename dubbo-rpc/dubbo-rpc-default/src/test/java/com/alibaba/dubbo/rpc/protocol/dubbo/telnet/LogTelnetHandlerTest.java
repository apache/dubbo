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

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * LogTelnetHandlerTest.java
 *
 * @author tony.chenl
 */
public class LogTelnetHandlerTest {

    private static TelnetHandler log = new LogTelnetHandler();
    private Channel mockChannel;

    @Test
    public void testChangeLogLevel() throws RemotingException {
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.replay(mockChannel);
        String result = log.telnet(mockChannel, "error");
        assertTrue(result.contains("\r\nCURRENT LOG LEVEL:ERROR"));
        String result2 = log.telnet(mockChannel, "warn");
        assertTrue(result2.contains("\r\nCURRENT LOG LEVEL:WARN"));
        EasyMock.reset(mockChannel);
    }

    @Test
    public void testPrintLog() throws RemotingException {
        mockChannel = EasyMock.createMock(Channel.class);
        EasyMock.replay(mockChannel);
        String result = log.telnet(mockChannel, "100");
        assertTrue(result.contains("CURRENT LOG APPENDER"));
        EasyMock.reset(mockChannel);
    }

}