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

import io.netty.channel.Channel;
import io.netty.util.DefaultAttributeMap;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.legacy.ProtocolUtils;
import org.apache.dubbo.qos.legacy.service.DemoService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

public class ChangeTelnetTest {
    private final DefaultAttributeMap defaultAttributeMap = new DefaultAttributeMap();
    private static final BaseCommand change = new ChangeTelnet(FrameworkModel.defaultModel());

    private Channel mockChannel;
    private CommandContext mockCommandContext;
    private Invoker<DemoService> mockInvoker;

    @AfterAll
    public static void tearDown() {

    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() {
        mockCommandContext = mock(CommandContext.class);
        mockChannel = mock(Channel.class);
        mockInvoker = mock(Invoker.class);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        mockChannel.attr(ChangeTelnet.SERVICE_KEY).set("org.apache.dubbo.rpc.protocol.dubbo.support.DemoService");
        given(mockCommandContext.getRemote()).willReturn(mockChannel);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:20884/demo"));
    }

    @AfterEach
    public void after() {
        ProtocolUtils.closeAll();
        reset(mockCommandContext, mockChannel, mockInvoker);
    }

    @Test
    public void testChangeSimpleName() {
        ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME).export(mockInvoker);
        String result = change.execute(mockCommandContext, new String[]{"DemoService"});
        assertEquals("Used the DemoService as default.\r\nYou can cancel default service by command: cd /", result);
    }

    @Test
    public void testChangeName() {
        ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME).export(mockInvoker);
        String result = change.execute(mockCommandContext, new String[]{"org.apache.dubbo.qos.legacy.service.DemoService"});
        assertEquals("Used the org.apache.dubbo.qos.legacy.service.DemoService as default.\r\nYou can cancel default service by command: cd /",
            result);
    }

    @Test
    public void testChangePath() {
        ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME).export(mockInvoker);
        String result = change.execute(mockCommandContext, new String[]{"demo"});
        assertEquals("Used the demo as default.\r\nYou can cancel default service by command: cd /", result);
    }

    @Test
    public void testChangeMessageNull() {
        String result = change.execute(mockCommandContext, null);
        assertEquals("Please input service name, eg: \r\ncd XxxService\r\ncd com.xxx.XxxService", result);
    }

    @Test
    public void testChangeServiceNotExport() {
        String result = change.execute(mockCommandContext, new String[]{"demo"});
        assertEquals("No such service demo", result);
    }

    @Test
    public void testChangeCancel() {
        String result = change.execute(mockCommandContext, new String[]{".."});
        assertEquals("Cancelled default service org.apache.dubbo.rpc.protocol.dubbo.support.DemoService.", result);
    }

    @Test
    public void testChangeCancel2() {
        String result = change.execute(mockCommandContext, new String[]{"/"});
        assertEquals("Cancelled default service org.apache.dubbo.rpc.protocol.dubbo.support.DemoService.", result);
    }
}
