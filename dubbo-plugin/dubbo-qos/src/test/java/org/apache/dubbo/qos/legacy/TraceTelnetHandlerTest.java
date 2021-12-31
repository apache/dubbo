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
package org.apache.dubbo.qos.legacy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.qos.legacy.service.DemoService;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.filter.TraceFilter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

public class TraceTelnetHandlerTest {

    private TelnetHandler handler;
    private Channel mockChannel;
    private Invoker<DemoService> mockInvoker;
    private URL url = URL.valueOf("dubbo://127.0.0.1:20884/demo");

    @BeforeEach
    public void setUp() {
        handler = new TraceTelnetHandler();
        mockChannel = mock(Channel.class);
        mockInvoker = mock(Invoker.class);
        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvoker.getUrl()).willReturn(url);
    }

    @AfterEach
    public void tearDown() {
        reset(mockChannel, mockInvoker);
        FrameworkModel.destroyAll();
    }

    @Test
    public void testTraceTelnetAddTracer() throws Exception {
        String method = "sayHello";
        String message = "org.apache.dubbo.qos.legacy.service.DemoService sayHello 1";
        Class<?> type = DemoService.class;

        ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME).export(mockInvoker);
        handler.telnet(mockChannel, message);

        String key = type.getName() + "." + method;
        Field tracers = TraceFilter.class.getDeclaredField("TRACERS");
        tracers.setAccessible(true);
        ConcurrentHashMap<String, Set<Channel>> map =
                (ConcurrentHashMap<String, Set<Channel>>) tracers.get(new ConcurrentHashMap<String, Set<Channel>>());

        Set<Channel> channels = map.getOrDefault(key, null);
        Assertions.assertNotNull(channels);

        Assertions.assertTrue(channels.contains(mockChannel));
    }
}
