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
import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
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

import org.apache.dubbo.rpc.protocol.rest.annotation.metadata.MetadataResolver;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.exception.DoublePathCheckException;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionHandler;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;

import org.apache.dubbo.rpc.protocol.rest.rest.AnotherUserRestService;
import org.apache.dubbo.rpc.protocol.rest.rest.AnotherUserRestServiceImpl;
import org.apache.dubbo.rpc.protocol.rest.rest.HttpMethodService;
import org.apache.dubbo.rpc.protocol.rest.rest.HttpMethodServiceImpl;

import org.apache.dubbo.rpc.protocol.rest.rest.RestDemoForTestException;
import org.hamcrest.CoreMatchers;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXCEPTION_MAPPER_KEY;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXTENSION_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class JaxrsRestProtocolTest {
    private final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private final ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private final int availablePort = NetUtils.getAvailablePort();
    private final URL exportUrl = URL.valueOf("rest://127.0.0.1:" + availablePort + "/rest?interface=org.apache.dubbo.rpc.protocol.rest.DemoService");
    private final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
    private final ExceptionMapper exceptionMapper = new ExceptionMapper();

    @AfterEach
    public void tearDown() {
        protocol.destroy();
        FrameworkModel.destroyAll();
    }

    @Test
    void testRestProtocol() {
        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort() + "/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.DemoService");

        DemoServiceImpl server = new DemoServiceImpl();

        url = this.registerProvider(url, server, DemoService.class);

        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, url));
        Invoker<DemoService> invoker = protocol.refer(DemoService.class, url);
        Assertions.assertFalse(server.isCalled());

        DemoService client = proxy.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);

        String header = client.header("header test");
        Assertions.assertEquals("header test", header);

        Assertions.assertEquals(1, client.headerInt(1));
        invoker.destroy();
        exporter.unexport();
    }


    @Test
    void testAnotherUserRestProtocolByDifferentRestClient() {
        testAnotherUserRestProtocol(org.apache.dubbo.remoting.Constants.OK_HTTP);
        testAnotherUserRestProtocol(org.apache.dubbo.remoting.Constants.APACHE_HTTP_CLIENT);
        testAnotherUserRestProtocol(org.apache.dubbo.remoting.Constants.URL_CONNECTION);
    }


    void testAnotherUserRestProtocol(String restClient) {
        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort()
            + "/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.rest.AnotherUserRestService&"
            + org.apache.dubbo.remoting.Constants.CLIENT_KEY + "=" + restClient);

        AnotherUserRestServiceImpl server = new AnotherUserRestServiceImpl();

        url = this.registerProvider(url, server, DemoService.class);

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

        HashMap<String, String> map = new HashMap<>();
        map.put("headers", "h1");
        Assertions.assertEquals("h1", client.headerMap(map));
        Assertions.assertEquals(null, client.headerMap(null));


        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testRestProtocolWithContextPath() {
        DemoServiceImpl server = new DemoServiceImpl();
        Assertions.assertFalse(server.isCalled());
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("rest://127.0.0.1:" + port + "/a/b/c?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.DemoService");

        url = this.registerProvider(url, server, DemoService.class);

        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, url));

        url = URL.valueOf("rest://127.0.0.1:" + port + "/a/b/c/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.DemoService");
        Invoker<DemoService> invoker = protocol.refer(DemoService.class, url);
        DemoService client = proxy.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testExport() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        RpcContext.getClientAttachment().setAttachment("timeout", "20000");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, url));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, url));

        Integer echoString = demoService.hello(1, 2);
        assertThat(echoString, is(3));

        exporter.unexport();
    }

    @Test
    void testNettyServer() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(new DemoServiceImpl(), DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

        Integer echoString = demoService.hello(10, 10);
        assertThat(echoString, is(20));

        exporter.unexport();
    }

    @Disabled
    @Test
    void testServletWithoutWebConfig() {
        Assertions.assertThrows(RpcException.class, () -> {
            DemoService server = new DemoServiceImpl();

            URL url = this.registerProvider(exportUrl, server, DemoService.class);

            URL servletUrl = url.addParameter(SERVER_KEY, "servlet");

            protocol.export(proxy.getInvoker(server, DemoService.class, servletUrl));
        });
    }

    @Test
    void testErrorHandler() {
        Assertions.assertThrows(RpcException.class, () -> {
            exceptionMapper.unRegisterMapper(RuntimeException.class);
            DemoService server = new DemoServiceImpl();

            URL url = this.registerProvider(exportUrl, server, DemoService.class);

            URL nettyUrl = url.addParameter(SERVER_KEY, "netty");
            Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

            DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

            demoService.error();
        });
    }

    @Test
    void testInvoke() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, url));

        RpcInvocation rpcInvocation = new RpcInvocation("hello", DemoService.class.getName(), "", new Class[]{Integer.class, Integer.class}, new Integer[]{2, 3});

        Result result = exporter.getInvoker().invoke(rpcInvocation);
        assertThat(result.getValue(), CoreMatchers.<Object>is(5));
    }

    @Test
    void testFilter() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty")
            .addParameter(EXTENSION_KEY, "org.apache.dubbo.rpc.protocol.rest.support.LoggingFilter");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

        Integer result = demoService.hello(1, 2);

        assertThat(result, is(3));

        exporter.unexport();
    }

    @Test
    void testRpcContextFilter() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        // use RpcContextFilter
        URL nettyUrl = url.addParameter(SERVER_KEY, "netty")
            .addParameter(EXTENSION_KEY, "org.apache.dubbo.rpc.protocol.rest.RpcContextFilter");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

        // make sure null and base64 encoded string can work
        RpcContext.getClientAttachment().setAttachment("key1", null);
        RpcContext.getClientAttachment().setAttachment("key2", "value");
        RpcContext.getClientAttachment().setAttachment("key3", "=value");
        RpcContext.getClientAttachment().setAttachment("key4", "YWJjZGVmCg==");
        RpcContext.getClientAttachment().setAttachment("key5", "val=ue");
        Integer result = demoService.hello(1, 2);

        assertThat(result, is(3));

        Map<String, Object> attachment = DemoServiceImpl.getAttachments();
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
            DemoService server = new DemoServiceImpl();

            URL url = this.registerProvider(exportUrl, server, DemoService.class);

            URL nettyUrl = url.addParameter(EXTENSION_KEY, "com.not.existing.Filter");
            protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));
        });
    }

    @Test
    void testDefaultPort() {
        assertThat(protocol.getDefaultPort(), is(80));
    }

    @Test
    void testExceptionMapper() {

        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL exceptionUrl = url.addParameter(EXTENSION_KEY, TestExceptionMapper.class.getName());

        protocol.export(proxy.getInvoker(server, DemoService.class, exceptionUrl));

        DemoService referDemoService = this.proxy.getProxy(protocol.refer(DemoService.class, exceptionUrl));

        Assertions.assertEquals("test-exception", referDemoService.error());
    }

    @Test
    void testFormConsumerParser() {
        DemoService server = new DemoServiceImpl();
        URL nettyUrl = this.registerProvider(exportUrl, server, DemoService.class);


        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));


        Long number = demoService.testFormBody(18l);
        Assertions.assertEquals(18l, number);

        exporter.unexport();
    }

    @Test
    void test404() {
        Assertions.assertThrows(RpcException.class, () -> {
            DemoService server = new DemoServiceImpl();
            URL nettyUrl = this.registerProvider(exportUrl, server, DemoService.class);


            Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

            URL referUrl = URL.valueOf("rest://127.0.0.1:" + availablePort + "/rest?interface=org.apache.dubbo.rpc.protocol.rest.rest.RestDemoForTestException");

            RestDemoForTestException restDemoForTestException = this.proxy.getProxy(protocol.refer(RestDemoForTestException.class, referUrl));

            restDemoForTestException.test404();

            exporter.unexport();
        });

    }

    @Test
    void test400() {
        Assertions.assertThrows(RpcException.class, () -> {
            DemoService server = new DemoServiceImpl();
            URL nettyUrl = this.registerProvider(exportUrl, server, DemoService.class);


            Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

            URL referUrl = URL.valueOf("rest://127.0.0.1:" + availablePort + "/rest?interface=org.apache.dubbo.rpc.protocol.rest.rest.RestDemoForTestException");

            RestDemoForTestException restDemoForTestException = this.proxy.getProxy(protocol.refer(RestDemoForTestException.class, referUrl));

            restDemoForTestException.test400("abc", "edf");

            exporter.unexport();
        });

    }

    @Test
    void testPrimitive() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty")
            .addParameter(EXTENSION_KEY, "org.apache.dubbo.rpc.protocol.rest.support.LoggingFilter");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

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


    @Test
    void testDoubleCheckException() {


        Assertions.assertThrows(DoublePathCheckException.class, () -> {

            DemoService server = new DemoServiceImpl();


            Invoker<DemoService> invoker = proxy.getInvoker(server, DemoService.class, exportUrl);

            PathAndInvokerMapper pathAndInvokerMapper = new PathAndInvokerMapper();

            ServiceRestMetadata serviceRestMetadata = MetadataResolver.resolveConsumerServiceMetadata(DemoService.class, exportUrl, "");

            Map<PathMatcher, RestMethodMetadata> pathContainPathVariableToServiceMap = serviceRestMetadata.getPathUnContainPathVariableToServiceMap();


            pathAndInvokerMapper.addPathAndInvoker(pathContainPathVariableToServiceMap, invoker);
            pathAndInvokerMapper.addPathAndInvoker(pathContainPathVariableToServiceMap, invoker);
        });


    }

    @Test
    void testMapParam() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty")
            .addParameter(EXTENSION_KEY, "org.apache.dubbo.rpc.protocol.rest.support.LoggingFilter");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

        Map<String, String> params = new HashMap<>();
        params.put("param", "P1");
        ;


        Map<String, String> headers = new HashMap<>();
        headers.put("header", "H1");


        Assertions.assertEquals("P1", demoService.testMapParam(params));
        Assertions.assertEquals("H1", demoService.testMapHeader(headers));

        MultivaluedMapImpl<String, String> forms = new MultivaluedMapImpl<>();
        forms.put("form", Arrays.asList("F1"));

        Assertions.assertEquals(Arrays.asList("F1"), demoService.testMapForm(forms));
        exporter.unexport();
    }


    @Test
    void testNoArgParam() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty")
            .addParameter(EXTENSION_KEY, "org.apache.dubbo.rpc.protocol.rest.support.LoggingFilter");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));


        Assertions.assertEquals(null, demoService.noStringHeader(null));
        Assertions.assertEquals(null, demoService.noStringParam(null));
        Assertions.assertThrows(RpcException.class, () -> {
            demoService.noIntHeader(1);
        });

        Assertions.assertThrows(RpcException.class, () -> {
            demoService.noIntParam(1);
        });

        Assertions.assertEquals(null, demoService.noBodyArg(null));
        exporter.unexport();
    }

    @Test
    void testToken() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL nettyUrl = url.addParameter(RestConstant.TOKEN_KEY, "TOKEN");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));


        Assertions.assertEquals("Hello, hello", demoService.sayHello("hello"));
        exporter.unexport();
    }


    @Test
    void testHttpMethods() {
        testHttpMethod(org.apache.dubbo.remoting.Constants.OK_HTTP);
        testHttpMethod(org.apache.dubbo.remoting.Constants.APACHE_HTTP_CLIENT);
        testHttpMethod(org.apache.dubbo.remoting.Constants.URL_CONNECTION);
    }

    void testHttpMethod(String restClient) {
        HttpMethodService server = new HttpMethodServiceImpl();

        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort()
            + "/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.rest.HttpMethodService&"
            + org.apache.dubbo.remoting.Constants.CLIENT_KEY + "=" + restClient);
        url = this.registerProvider(url, server, HttpMethodService.class);
        Exporter<HttpMethodService> exporter = protocol.export(proxy.getInvoker(server, HttpMethodService.class, url));

        HttpMethodService demoService = this.proxy.getProxy(protocol.refer(HttpMethodService.class, url));


        String expect = "hello";
        Assertions.assertEquals(null, demoService.sayHelloHead());
        Assertions.assertEquals(expect, demoService.sayHelloDelete("hello"));
        Assertions.assertEquals(expect, demoService.sayHelloGet("hello"));
        Assertions.assertEquals(expect, demoService.sayHelloOptions("hello"));
//        Assertions.assertEquals(expect, demoService.sayHelloPatch("hello"));
        Assertions.assertEquals(expect, demoService.sayHelloPost("hello"));
        Assertions.assertEquals(expect, demoService.sayHelloPut("hello"));
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
