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
package org.apache.dubbo.remoting.telnet.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class TelnetHandlerAdapterTest {

    @Test
    public void testTelnet() throws RemotingException {

        Channel channel = Mockito.mock(Channel.class);
        Map<String, String> param = new HashMap<>();
        param.put("telnet", "status");
        URL url = new URL("p1", "127.0.0.1", 12345, "path1", param);
        Mockito.when(channel.getUrl()).thenReturn(url);
        TelnetHandlerAdapter telnetHandlerAdapter = new TelnetHandlerAdapter();

        String message = "--no-prompt status ";
        String expectedResult = "OK\r\n";
        Assertions.assertEquals(expectedResult, telnetHandlerAdapter.telnet(channel, message));

        message = "--no-prompt status test";
        expectedResult = "Unsupported parameter test for status.\r\n";
        Assertions.assertEquals(expectedResult, telnetHandlerAdapter.telnet(channel, message));

        message = "--no-prompt test";
        expectedResult = "Unsupported command: test\r\n";
        Assertions.assertEquals(expectedResult, telnetHandlerAdapter.telnet(channel, message));

        message = "--no-prompt help";
        expectedResult = "Command: help disabled\r\n";
        Assertions.assertEquals(expectedResult, telnetHandlerAdapter.telnet(channel, message));

        message = "--no-prompt";
        expectedResult = StringUtils.EMPTY_STRING;
        Assertions.assertEquals(expectedResult, telnetHandlerAdapter.telnet(channel, message));

        message = "help";
        expectedResult = "Command: help disabled\r\ndubbo>";
        Assertions.assertEquals(expectedResult, telnetHandlerAdapter.telnet(channel, message));
    }
}
