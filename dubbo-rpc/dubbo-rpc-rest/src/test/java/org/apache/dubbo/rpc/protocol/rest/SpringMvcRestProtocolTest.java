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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionHandler;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;
import org.apache.dubbo.rpc.protocol.rest.mvc.SpringDemoServiceImpl;

import org.apache.dubbo.rpc.protocol.rest.mvc.SpringRestDemoService;
import org.apache.dubbo.rpc.protocol.rest.rest.AnotherUserRestService;
import org.apache.dubbo.rpc.protocol.rest.rest.AnotherUserRestServiceImpl;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Arrays;
import java.util.Map;

import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXTENSION_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpringMvcRestProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private final static int availablePort = NetUtils.getAvailablePort();
    private final static URL exportUrl = URL.valueOf("rest://127.0.0.1:" + availablePort + "/rest?interface=org.apache.dubbo.rpc.protocol.rest.mvc.SpringRestDemoService");


    private final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    private final ExceptionMapper exceptionMapper = new ExceptionMapper();

    @AfterEach
    public void tearDown() {
        protocol.destroy();
        FrameworkModel.destroyAll();
    }

    public SpringRestDemoService getServerImpl() {
        return new SpringDemoServiceImpl();
    }


    public Class<SpringRestDemoService> getServerClass() {
        return SpringRestDemoService.class;
    }

    public Exporter<SpringRestDemoService> getExport(URL url, SpringRestDemoService server) {
        url = url.addParameter(SERVER_KEY, Constants.NETTY_HTTP);
        return protocol.export(proxy.getInvoker(server, getServerClass(), url));
    }

    public Exporter<SpringRestDemoService> getExceptionHandlerExport(URL url, SpringRestDemoService server) {
        url = url.addParameter(SERVER_KEY, Constants.NETTY_HTTP);
        url = url.addParameter(EXTENSION_KEY, TestExceptionMapper.class.getName());
        return protocol.export(proxy.getInvoker(server, getServerClass(), url));
    }


    @Test
    void testRestProtocol() {
        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort() + "/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.mvc.SpringRestDemoService");

        SpringRestDemoService server = getServerImpl();

        url = this.registerProvider(url, server, getServerClass());

        Exporter<SpringRestDemoService> exporter = getExport(url, server);
        Invoker<SpringRestDemoService> invoker = protocol.refer(SpringRestDemoService.class, url);
        Assertions.assertFalse(server.isCalled());

        SpringRestDemoService client = proxy.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);

        String header = client.testHeader("header");
        Assertions.assertEquals("header", header);

        String headerInt = client.testHeaderInt(1);
        Assertions.assertEquals("1", headerInt);
        invoker.destroy();
        exporter.unexport();
    }


    @Test
    void testAnotherUserRestProtocol() {
        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort() + "/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.rest.AnotherUserRestService");

        AnotherUserRestServiceImpl server = new AnotherUserRestServiceImpl();

        url = this.registerProvider(url, server, SpringRestDemoService.class);

        Exporter<AnotherUserRestService> exporter = protocol.export(proxy.getInvoker(server, AnotherUserRestService.class, url));
        Invoker<AnotherUserRestService> invoker = protocol.refer(AnotherUserRestService.class, url);


        AnotherUserRestService client = proxy.getProxy(invoker);
        User result = client.getUser(123l);

        Assertions.assertEquals(123l, result.getId());

        result.setName("dubbo");
        Assertions.assertEquals(123l, client.registerUser(result).getId());

        Assertions.assertEquals("context", client.getContext());

        byte[] bytes = {1, 2, 3, 4};
        Assertions.assertTrue(Arrays.equals(bytes, client.bytes(bytes)));

        Assertions.assertEquals(1l, client.number(1l));

        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testRestProtocolWithContextPath() {
        SpringRestDemoService server = getServerImpl();
        Assertions.assertFalse(server.isCalled());
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("rest://127.0.0.1:" + port + "/a/b/c?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.mvc.SpringRestDemoService");

        url = this.registerProvider(url, server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExport(url, server);

        url = URL.valueOf("rest://127.0.0.1:" + port + "/a/b/c/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.mvc.SpringRestDemoService");
        Invoker<SpringRestDemoService> invoker = protocol.refer(SpringRestDemoService.class, url);
        SpringRestDemoService client = proxy.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testExport() {
        SpringRestDemoService server = getServerImpl();

        URL url = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

        RpcContext.getClientAttachment().setAttachment("timeout", "200");
        Exporter<SpringRestDemoService> exporter = getExport(url, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, url));

        Integer echoString = demoService.hello(1, 2);
        assertThat(echoString, is(3));

        exporter.unexport();
    }

    @Test
    void testNettyServer() {
        SpringRestDemoService server = getServerImpl();

        URL nettyUrl = this.registerProvider(exportUrl, server, SpringRestDemoService.class);


        Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, nettyUrl));

        Integer echoString = demoService.hello(10, 10);
        assertThat(echoString, is(20));

        exporter.unexport();
    }

    @Disabled
    @Test
    void testServletWithoutWebConfig() {
        Assertions.assertThrows(RpcException.class, () -> {
            SpringRestDemoService server = getServerImpl();

            URL url = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

            URL servletUrl = url.addParameter(SERVER_KEY, "servlet");

            protocol.export(proxy.getInvoker(server, getServerClass(), servletUrl));
        });
    }

    @Test
    void testErrorHandler() {
        Assertions.assertThrows(RpcException.class, () -> {
            exceptionMapper.unRegisterMapper(RuntimeException.class);
            SpringRestDemoService server = getServerImpl();

            URL nettyUrl = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

            Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);

            SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, nettyUrl));

            demoService.error();
        });
    }

    @Test
    void testInvoke() {
        SpringRestDemoService server = getServerImpl();

        URL url = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExport(url, server);

        RpcInvocation rpcInvocation = new RpcInvocation("hello", SpringRestDemoService.class.getName(), "", new Class[]{Integer.class, Integer.class}, new Integer[]{2, 3});

        Result result = exporter.getInvoker().invoke(rpcInvocation);
        assertThat(result.getValue(), CoreMatchers.<Object>is(5));
    }

    @Test
    void testFilter() {
        SpringRestDemoService server = getServerImpl();

        URL url = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExport(url, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, url));

        Integer result = demoService.hello(1, 2);

        assertThat(result, is(3));

        exporter.unexport();
    }

    @Test
    void testRpcContextFilter() {
        SpringRestDemoService server = getServerImpl();

        URL nettyUrl = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

        // use RpcContextFilter
//        URL nettyUrl = url.addParameter(SERVER_KEY, "netty")
//            .addParameter(EXTENSION_KEY, "org.apache.dubbo.rpc.protocol.rest.RpcContextFilter");
        Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, nettyUrl));

        // make sure null and base64 encoded string can work
        RpcContext.getClientAttachment().setAttachment("key1", null);
        RpcContext.getClientAttachment().setAttachment("key2", "value");
        RpcContext.getClientAttachment().setAttachment("key3", "=value");
        RpcContext.getClientAttachment().setAttachment("key4", "YWJjZGVmCg==");
        RpcContext.getClientAttachment().setAttachment("key5", "val=ue");
        Integer result = demoService.hello(1, 2);

        assertThat(result, is(3));

        Map<String, Object> attachment = org.apache.dubbo.rpc.protocol.rest.mvc.SpringDemoServiceImpl.getAttachments();
        assertThat(attachment.get("key1"), nullValue());
        assertThat(attachment.get("key2"), equalTo("value"));
        assertThat(attachment.get("key3"), equalTo("=value"));
        assertThat(attachment.get("key4"), equalTo("YWJjZGVmCg=="));
        assertThat(attachment.get("key5"), equalTo("val=ue"));

        exporter.unexport();
    }

    @Disabled
    @Test
    void testRegFail() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SpringRestDemoService server = getServerImpl();

            URL url = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

            URL nettyUrl = url.addParameter(EXTENSION_KEY, "com.not.existing.Filter");
            Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);
        });
    }

    @Test
    void testDefaultPort() {
        assertThat(protocol.getDefaultPort(), is(80));
    }


    @Test
    void testExceptionMapper() {

        SpringRestDemoService server = getServerImpl();

        URL exceptionUrl = this.registerProvider(exportUrl, server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExceptionHandlerExport(exceptionUrl, server);


        SpringRestDemoService referDemoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, exceptionUrl));

        Assertions.assertEquals("test-exception", referDemoService.error());

        exporter.unexport();
    }


    @Test
    void testFormConsumerParser() {
        SpringRestDemoService server = getServerImpl();

        URL nettyUrl = this.registerProvider(exportUrl, server, SpringRestDemoService.class);


        Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, nettyUrl));

        User user = new User();
        user.setAge(18);
        user.setName("dubbo");
        user.setId(404l);
        String name = demoService.testFormBody(user);
        Assertions.assertEquals("dubbo", name);

        LinkedMultiValueMap<String, String> forms = new LinkedMultiValueMap<>();
        forms.put("form", Arrays.asList("F1"));

        Assertions.assertEquals(Arrays.asList("F1"), demoService.testFormMapBody(forms));

        exporter.unexport();
    }

    @Test
    void testPrimitive() {
        SpringRestDemoService server = getServerImpl();

        URL nettyUrl = this.registerProvider(exportUrl, server, SpringRestDemoService.class);


        Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, nettyUrl));

        Integer result = demoService.primitiveInt(1, 2);
        Long resultLong = demoService.primitiveLong(1, 2l);
        long resultByte = demoService.primitiveByte((byte) 1, 2l);
        long resultShort = demoService.primitiveShort((short) 1, 2l, 1);

        assertThat(result, is(3));
        assertThat(resultShort, is(3l));
        assertThat(resultLong, is(3l));
        assertThat(resultByte, is(3l));

        exporter.unexport();
    }

    public static class TestExceptionMapper implements ExceptionHandler<RuntimeException> {

        @Override
        public String result(RuntimeException e) {
            return "test-exception";
        }
    }

    private URL registerProvider(URL url, Object impl, Class<?> interfaceClass) {
        ServiceDescriptor serviceDescriptor = repository.registerService(interfaceClass);
        ProviderModel providerModel = new ProviderModel(
            url.getServiceKey(),
            impl,
            serviceDescriptor,
            null,
            null);
        repository.registerProvider(providerModel);
        return url.setServiceModel(providerModel);
    }
}
