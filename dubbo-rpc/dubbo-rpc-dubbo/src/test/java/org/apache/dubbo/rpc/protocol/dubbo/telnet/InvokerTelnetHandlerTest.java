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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * InvokeTelnetHandlerTest.java
 */
public class InvokerTelnetHandlerTest {

    private static TelnetHandler invoke = new InvokeTelnetHandler();
    private static TelnetHandler select = new SelectTelnetHandler();
    private Channel mockChannel;

    @BeforeEach
    public void setup() {
        ApplicationModel.reset();
    }

    @AfterEach
    public void after() {
        ProtocolUtils.closeAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeDefaultService() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        ApplicationModel.initProviderModel(DemoService.class.getName(), providerModel);

        String result = invoke.telnet(mockChannel, "echo(\"ok\")");
        assertTrue(result.contains("result: \"ok\""));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithSpecifyService() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        ApplicationModel.initProviderModel(DemoService.class.getName(), providerModel);

        String result = invoke.telnet(mockChannel, "DemoService.echo(\"ok\")");
        assertTrue(result.contains("result: \"ok\""));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeByPassingNullValue() {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        ApplicationModel.initProviderModel(DemoService.class.getName(), providerModel);
        try {
            invoke.telnet(mockChannel, "sayHello(null)");
        } catch (Exception ex) {
            assertTrue(ex instanceof NullPointerException);
        }
    }

    @Test
    public void testInvokeByPassingEnumValue() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        ApplicationModel.initProviderModel(DemoService.class.getName(), providerModel);

        String result = invoke.telnet(mockChannel, "getType(\"High\")");
        assertTrue(result.contains("result: \"High\""));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testOverriddenMethodWithSpecifyParamType() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        ApplicationModel.initProviderModel(DemoService.class.getName(), providerModel);
        String result = invoke.telnet(mockChannel, "getPerson({\"name\":\"zhangsan\",\"age\":12,\"class\":\"org.apache.dubbo.rpc.protocol.dubbo.support.Person\"})");
        assertTrue(result.contains("result: 12"));
    }

    @Test
    public void testInvokeOverriddenMethodBySelect() throws RemotingException {
        //create a real instance to keep the attribute values;
        mockChannel = spy(getChannelInstance());
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        ApplicationModel.initProviderModel(DemoService.class.getName(), providerModel);
        String param = "{\"name\":\"Dubbo\",\"age\":8}";
        String result = invoke.telnet(mockChannel, "getPerson(" + param + ")");
        assertTrue(result.contains("Please use the select command to select the method you want to invoke. eg: select 1"));
        result = select.telnet(mockChannel, "1");
        //result dependent on method order.
        assertTrue(result.contains("result: 8") || result.contains("result: \"Dubbo\""));
    }

    @Test
    public void testInvokeMultiJsonParamMethod() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(), new DemoServiceImpl(), DemoService.class);
        ApplicationModel.initProviderModel(DemoService.class.getName(), providerModel);
        String param = "{\"name\":\"Dubbo\",\"age\":8},{\"name\":\"Apache\",\"age\":20}";
        String result = invoke.telnet(mockChannel, "getPerson(" + param + ")");
        assertTrue(result.contains("result: 28"));
    }

    @Test
    public void testMessageNull() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);

        String result = invoke.telnet(mockChannel, null);
        assertEquals("Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})",
                result);
    }

    @Test
    public void testInvalidMessage() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);
        String result = invoke.telnet(mockChannel, "(");
        assertEquals("Invalid parameters, format: service.method(args)", result);
    }

    private Channel getChannelInstance() {
        return new Channel() {
            private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public boolean isConnected() {
                return false;
            }

            @Override
            public boolean hasAttribute(String key) {
                return attributes.containsKey(key);
            }

            @Override
            public Object getAttribute(String key) {
                return attributes.get(key);
            }

            @Override
            public void setAttribute(String key, Object value) {
                if (value == null) { // The null value unallowed in the ConcurrentHashMap.
                    attributes.remove(key);
                } else {
                    attributes.put(key, value);
                }
            }

            @Override
            public void removeAttribute(String key) {
                attributes.remove(key);
            }


            @Override
            public URL getUrl() {
                return null;
            }

            @Override
            public ChannelHandler getChannelHandler() {
                return null;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            @Override
            public void send(Object message) throws RemotingException {

            }

            @Override
            public void send(Object message, boolean sent) throws RemotingException {

            }

            @Override
            public void close() {

            }

            @Override
            public void close(int timeout) {

            }

            @Override
            public void startClose() {

            }

            @Override
            public boolean isClosed() {
                return false;
            }
        };
    }
}
