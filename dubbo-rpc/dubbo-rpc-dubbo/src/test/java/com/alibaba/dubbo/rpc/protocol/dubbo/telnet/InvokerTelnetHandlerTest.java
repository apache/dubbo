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
package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * CountTelnetHandlerTest.java
 */
public class InvokerTelnetHandlerTest {

    private static TelnetHandler invoke = new InvokeTelnetHandler();
    private static TelnetHandler select = new SelectTelnetHandler();
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();


    private Channel mockChannel;

    @After
    public void after() {
        ProtocolUtils.closeAll();
    }

    @Before
    public void setUp() {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("dubbo://127.0.0.1:20886/demo").addParameter(Constants.INTERFACE_KEY, DemoService.class.getName())));

    }

    @Test
    public void testInvokeDefaultSService() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn("com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService");
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));
        String result = invoke.telnet(mockChannel, "DemoService.echo(\"ok\")");
        assertTrue(result.contains("Use default service com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService.\r\n\"ok\"\r\n"));
    }

    @Test
    public void testInvokeDefaultService() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));
        String result = invoke.telnet(mockChannel, "echo(\"ok\")");
        assertTrue(result.contains("\"ok\""));
    }


    @Test
    public void testInvokeByPassingNullValue() {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));
        try {
            invoke.telnet(mockChannel, "sayHello(null)");
        } catch (Exception ex) {
            assertTrue(ex instanceof NullPointerException);
        }
    }


    @Test
    public void testOverriddenMethodWithSpecifyParamType() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        String result = invoke.telnet(mockChannel, "getPerson({\"name\":\"zhangsan\",\"age\":12,\"class\":\"com.alibaba.dubbo.rpc.protocol.dubbo.support.Person\"})");
        assertTrue(result.contains("12"));
    }

    @Test
    public void testInvokeOverriddenMethodBySelect() throws RemotingException {
        //create a real instance to keep the attribute values;
        mockChannel = spy(getChannelInstance());
        given(mockChannel.getAttribute("telnet.service")).willReturn(DemoService.class.getName());
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));

        String param = "{\"name\":\"Dubbo\",\"age\":8}";
        String result = invoke.telnet(mockChannel, "getPerson(" + param + ")");
        assertTrue(result.contains("Please use the select command to select the method you want to invoke. eg: select 1"));
        result = select.telnet(mockChannel, "1");
        //result dependent on method order.
        assertTrue(result.contains("8") || result.contains("\"Dubbo\""));
    }

    @Test
    public void testInvokeMultiJsonParamMethod() throws RemotingException {
        mockChannel = mock(Channel.class);
        given(mockChannel.getAttribute("telnet.service")).willReturn(null);
        given(mockChannel.getLocalAddress()).willReturn(NetUtils.toAddress("127.0.0.1:5555"));
        given(mockChannel.getRemoteAddress()).willReturn(NetUtils.toAddress("127.0.0.1:20886"));


        String param = "{\"name\":\"Dubbo\",\"age\":8},{\"name\":\"Apache\",\"age\":20}";
        String result = invoke.telnet(mockChannel, "getPerson(" + param + ")");
        assertTrue(result.contains("28"));
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
    public void testInvaildMessage() throws RemotingException {
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
            public void send(Object message) {

            }

            @Override
            public void send(Object message, boolean sent) {

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