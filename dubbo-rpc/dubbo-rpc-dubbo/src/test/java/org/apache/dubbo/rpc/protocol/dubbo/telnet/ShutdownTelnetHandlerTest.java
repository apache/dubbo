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

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * SelectTelnetHandlerTest.java
 */
public class ShutdownTelnetHandlerTest {

    private static TelnetHandler handler = new ShutdownTelnetHandler();
    private Channel mockChannel;

    @SuppressWarnings("unchecked")
    @Test
    public void testInvoke() throws RemotingException {
        mockChannel = mock(Channel.class);
        String result = handler.telnet(mockChannel, "");
        assertTrue(result.contains("Application has shutdown successfully"));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithTimeParameter() throws RemotingException {
        mockChannel = mock(Channel.class);
        int sleepTime = 2000;
        long start = System.currentTimeMillis();
        String result = handler.telnet(mockChannel, "-t " + sleepTime);
        long end = System.currentTimeMillis();
        assertTrue(result.contains("Application has shutdown successfully") && (end - start) > sleepTime);
    }


}
