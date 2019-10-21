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

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.qos.legacy.service.DemoService;
import org.apache.dubbo.qos.legacy.service.DemoServiceImpl;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * SelectTelnetHandlerTest.java
 */
public class SelectTelnetHandlerTest {

    private static TelnetHandler select = new SelectTelnetHandler();
    private Channel mockChannel;
    List<Method> methods;
    private final ServiceRepository repository = ApplicationModel.getServiceRepository();

    @BeforeEach
    public void setup() {
        String methodName = "getPerson";
        methods = new ArrayList<>();
        for (Method method : DemoService.class.getMethods()) {
            if (method.getName().equals(methodName)) {
                methods.add(method);
            }
        }

        ApplicationModel.reset();
    }

    @AfterEach
    public void after() {
        ProtocolUtils.closeAll();
    }

    @Test
    public void testInvokeWithoutMethodList() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = select.telnet(mockChannel, "1");
        assertTrue(result.contains("Please use the invoke command first."));
    }

    @Test
    public void testInvokeWithIllegalMessage() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getAttribute(InvokeTelnetHandler.INVOKE_METHOD_LIST_KEY)).willReturn(methods);
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = select.telnet(mockChannel, "index");
        assertTrue(result.contains("Illegal index ,please input select 1"));

        result = select.telnet(mockChannel, "0");
        assertTrue(result.contains("Illegal index ,please input select 1"));

        result = select.telnet(mockChannel, "1000");
        assertTrue(result.contains("Illegal index ,please input select 1"));
    }

    @Test
    public void testInvokeWithNull() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getAttribute(InvokeTelnetHandler.INVOKE_METHOD_LIST_KEY)).willReturn(methods);
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = select.telnet(mockChannel, null);
        assertTrue(result.contains("Please input the index of the method you want to invoke"));
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
