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

import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.Channel;
import io.netty.util.DefaultAttributeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

public class PwdTelnetTest {
    private static final BaseCommand pwdTelnet = new PwdTelnet();
    private Channel mockChannel;
    private CommandContext mockCommandContext;

    private final DefaultAttributeMap defaultAttributeMap = new DefaultAttributeMap();

    @BeforeEach
    public void setUp() {
        mockChannel = mock(Channel.class);
        mockCommandContext = mock(CommandContext.class);
        given(mockCommandContext.getRemote()).willReturn(mockChannel);
        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
    }

    @AfterEach
    public void tearDown() {
        FrameworkModel.destroyAll();
        mockChannel.close();
        reset(mockChannel, mockCommandContext);
    }

    @Test
    public void testService() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");
        String result = pwdTelnet.execute(mockCommandContext, new String[0]);
        assertEquals("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService", result);
    }

    @Test
    public void testSlash() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(null);
        String result = pwdTelnet.execute(mockCommandContext, new String[0]);
        assertEquals("/", result);
    }

    @Test
    public void testMessageError() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(null);
        String result = pwdTelnet.execute(mockCommandContext, new String[]{"test"});
        assertEquals("Unsupported parameter [test] for pwd.", result);
    }
}
