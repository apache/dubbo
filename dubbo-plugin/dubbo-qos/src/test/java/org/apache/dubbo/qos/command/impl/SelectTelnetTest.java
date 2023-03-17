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

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.legacy.service.DemoService;
import org.apache.dubbo.qos.legacy.service.DemoServiceImpl;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import io.netty.channel.Channel;
import io.netty.util.DefaultAttributeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

class SelectTelnetTest {

    private BaseCommand select;

    private Channel mockChannel;
    private CommandContext mockCommandContext;

    private ModuleServiceRepository repository;
    private final DefaultAttributeMap defaultAttributeMap = new DefaultAttributeMap();
    private List<Method> methods;

    @BeforeEach
    public void setup() {
        repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
        select = new SelectTelnet(FrameworkModel.defaultModel());
        String methodName = "getPerson";
        methods = new ArrayList<>();
        for (Method method : DemoService.class.getMethods()) {
            if (method.getName().equals(methodName)) {
                methods.add(method);
            }
        }

        DubboBootstrap.reset();
        mockChannel = mock(Channel.class);
        mockCommandContext = mock(CommandContext.class);
        given(mockCommandContext.getRemote()).willReturn(mockChannel);
    }

    @AfterEach
    public void after() {
        FrameworkModel.destroyAll();
        reset(mockChannel, mockCommandContext);
    }

    @Test
    void testInvokeWithoutMethodList() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY)).willReturn(defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = select.execute(mockCommandContext, new String[]{"1"});
        assertTrue(result.contains("Please use the invoke command first."));

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).remove();
    }

    @Test
    void testInvokeWithIllegalMessage() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).set(methods);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY)).willReturn(defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = select.execute(mockCommandContext, new String[]{"index"});
        assertTrue(result.contains("Illegal index ,please input select 1"));

        result = select.execute(mockCommandContext, new String[]{"0"});
        assertTrue(result.contains("Illegal index ,please input select 1"));

        result = select.execute(mockCommandContext, new String[]{"1000"});
        assertTrue(result.contains("Illegal index ,please input select 1"));

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).remove();
    }

    @Test
    void testInvokeWithNull() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).set(methods);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY)).willReturn(defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = select.execute(mockCommandContext, new String[0]);
        assertTrue(result.contains("Please input the index of the method you want to invoke"));

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).remove();
    }

    private void registerProvider(String key, Object impl, Class<?> interfaceClass) {
        ServiceDescriptor serviceDescriptor = repository.registerService(interfaceClass);
        repository.registerProvider(
            key,
            impl,
            serviceDescriptor,
            null,
            null
        );
    }
}
