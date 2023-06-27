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

import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

class ShutdownTelnetTest {

    private BaseCommand shutdown;
    private Channel mockChannel;
    private CommandContext mockCommandContext;

    @BeforeEach
    public void setUp() {
        shutdown = new ShutdownTelnet(FrameworkModel.defaultModel());
        mockCommandContext = mock(CommandContext.class);
        mockChannel = mock(Channel.class);
        given(mockCommandContext.getRemote()).willReturn(mockChannel);
    }

    @AfterEach
    public void after() {
        FrameworkModel.destroyAll();
        reset(mockChannel, mockCommandContext);
    }

    @Test
    void testInvoke() throws RemotingException {
        String result = shutdown.execute(mockCommandContext, new String[0]);
        assertTrue(result.contains("Application has shutdown successfully"));
    }

    @Test
    void testInvokeWithTimeParameter() throws RemotingException {
        int sleepTime = 2000;
        long start = System.currentTimeMillis();
        String result = shutdown.execute(mockCommandContext, new String[]{"-t", "" + sleepTime});
        long end = System.currentTimeMillis();
        assertTrue(result.contains("Application has shutdown successfully"), result);
        assertTrue((end - start) >= sleepTime, "sleepTime: " + sleepTime + ", execTime: " + (end - start));
    }
}
