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
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

public class InvokeTelnetTest {

    private FrameworkModel frameworkModel;
    private BaseCommand invoke;
    private BaseCommand select;
    private Channel mockChannel;
    private CommandContext mockCommandContext;
    private final DefaultAttributeMap defaultAttributeMap = new DefaultAttributeMap();
    private ModuleServiceRepository repository;

    @BeforeEach
    public void setup() {
        DubboBootstrap.reset();
        frameworkModel = new FrameworkModel();
        invoke = new InvokeTelnet(frameworkModel);
        select = new SelectTelnet(frameworkModel);
        mockChannel = mock(Channel.class);
        mockCommandContext = mock(CommandContext.class);
        given(mockCommandContext.getRemote()).willReturn(mockChannel);
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);
        repository = applicationModel.getDefaultModule().getServiceRepository();
    }

    @AfterEach
    public void after() {
        frameworkModel.destroy();
        reset(mockChannel, mockCommandContext);
    }

    @Test
    public void testInvokeWithoutServicePrefixAndWithoutDefaultService() throws RemotingException {
        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        String result = invoke.execute(mockCommandContext, new String[]{"echo(\"ok\")"});
        assertTrue(result.contains("If you want to invoke like [invoke sayHello(\"xxxx\")], please execute cd command first," +
            " or you can execute it like [invoke IHelloService.sayHello(\"xxxx\")]"));
    }

    @Test
    public void testInvokeDefaultService() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);
        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));
        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        String result = invoke.execute(mockCommandContext, new String[]{"echo(\"ok\")"});
        assertTrue(result.contains("result: \"ok\""));
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testInvokeWithSpecifyService() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(null);
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        String result = invoke.execute(mockCommandContext, new String[]{"DemoService.echo(\"ok\")"});
        assertTrue(result.contains("result: \"ok\""));
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testInvokeByPassingNullValue() {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);
        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        try {
            invoke.execute(mockCommandContext, new String[]{"sayHello(null)"});
        } catch (Exception ex) {
            assertTrue(ex instanceof NullPointerException);
        }

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testInvokeByPassingEnumValue() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = invoke.execute(mockCommandContext, new String[]{"getType(\"High\")"});
        assertTrue(result.contains("result: \"High\""));
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testOverriddenMethodWithSpecifyParamType() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);
        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String result = invoke.execute(mockCommandContext,
            new String[]{"getPerson({\"name\":\"zhangsan\",\"age\":12,\"class\":\"org.apache.dubbo.qos.legacy.service.Person\"})"});
        assertTrue(result.contains("result: 12"));

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testInvokeOverriddenMethodBySelect() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);
        defaultAttributeMap.attr(SelectTelnet.SELECT_METHOD_KEY).set(null);
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_PROVIDER_KEY).set(null);
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).set(null);
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_MESSAGE_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_METHOD_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_METHOD_KEY));
        given(mockChannel.attr(InvokeTelnet.INVOKE_METHOD_PROVIDER_KEY)).willReturn(defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_PROVIDER_KEY));
        given(mockChannel.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY)).willReturn(defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY));
        given(mockChannel.attr(InvokeTelnet.INVOKE_MESSAGE_KEY)).willReturn(defaultAttributeMap.attr(InvokeTelnet.INVOKE_MESSAGE_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String param = "{\"name\":\"Dubbo\",\"age\":8}";
        String result = invoke.execute(mockCommandContext, new String[]{"getPerson(" + param + ")"});
        assertTrue(result.contains("Please use the select command to select the method you want to invoke. eg: select 1"));
        result = select.execute(mockCommandContext, new String[]{"1"});
        //result dependent on method order.
        assertTrue(result.contains("result: 8") || result.contains("result: \"Dubbo\""));
        result = select.execute(mockCommandContext, new String[]{"2"});
        assertTrue(result.contains("result: 8") || result.contains("result: \"Dubbo\""));

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_METHOD_KEY).remove();
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_PROVIDER_KEY).remove();
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).remove();
        defaultAttributeMap.attr(InvokeTelnet.INVOKE_MESSAGE_KEY).remove();
    }

    @Test
    public void testInvokeMethodWithMapParameter() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String param = "{1:\"Dubbo\",2:\"test\"}";
        String result = invoke.execute(mockCommandContext, new String[]{"getMap(" + param + ")"});
        assertTrue(result.contains("result: {1:\"Dubbo\",2:\"test\"}"));

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testInvokeMultiJsonParamMethod() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(DemoService.class.getName());
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        registerProvider(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);

        String param = "{\"name\":\"Dubbo\",\"age\":8},{\"name\":\"Apache\",\"age\":20}";
        String result = invoke.execute(mockCommandContext, new String[]{"getPerson(" + param + ")"});
        assertTrue(result.contains("result: 28"));

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testMessageNull() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(null);
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        String result = invoke.execute(mockCommandContext, new String[0]);
        assertEquals("Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})",
            result);

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
    }

    @Test
    public void testInvalidMessage() throws RemotingException {
        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).set(null);
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).set(null);

        given(mockChannel.attr(ChangeTelnet.SERVICE_KEY)).willReturn(defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY));
        given(mockChannel.attr(SelectTelnet.SELECT_KEY)).willReturn(defaultAttributeMap.attr(SelectTelnet.SELECT_KEY));

        String result = invoke.execute(mockCommandContext, new String[]{"("});
        assertEquals("Invalid parameters, format: service.method(args)", result);

        defaultAttributeMap.attr(ChangeTelnet.SERVICE_KEY).remove();
        defaultAttributeMap.attr(SelectTelnet.SELECT_KEY).remove();
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
