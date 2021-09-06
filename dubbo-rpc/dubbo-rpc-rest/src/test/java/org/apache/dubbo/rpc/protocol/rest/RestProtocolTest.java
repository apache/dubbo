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

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXTENSION_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class RestProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private final int availablePort = NetUtils.getAvailablePort();
    private final URL exportUrl = URL.valueOf("rest://127.0.0.1:" + availablePort + "/rest?interface=org.apache.dubbo.rpc.protocol.rest.DemoService");
    private final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    @AfterEach
    public void tearDown() {
        protocol.destroy();
        FrameworkModel.destroyAll();
    }

    @Test
    public void testRestProtocol() {
        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort() + "/rest/say?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.DemoService");
        DemoServiceImpl server = new DemoServiceImpl();

        url = this.registerProvider(url, server, DemoService.class);

        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, url));
        Invoker<DemoService> invoker = protocol.refer(DemoService.class, url);
        Assertions.assertFalse(server.isCalled());

        DemoService client = proxy.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testRestProtocolWithContextPath() {
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
    public void testExport() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        RpcContext.getClientAttachment().setAttachment("timeout", "200");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, url));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, url));

        Integer echoString = demoService.hello(1, 2);
        assertThat(echoString, is(3));

        exporter.unexport();
    }

    @Test
    public void testNettyServer() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty");
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(new DemoServiceImpl(), DemoService.class, nettyUrl));

        DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

        Integer echoString = demoService.hello(10, 10);
        assertThat(echoString, is(20));

        exporter.unexport();
    }

    @Test
    public void testServletWithoutWebConfig() {
        Assertions.assertThrows(RpcException.class, () -> {
            DemoService server = new DemoServiceImpl();

            URL url = this.registerProvider(exportUrl, server, DemoService.class);

            URL servletUrl = url.addParameter(SERVER_KEY, "servlet");

            protocol.export(proxy.getInvoker(server, DemoService.class, servletUrl));
        });
    }

    @Test
    public void testErrorHandler() {
        Assertions.assertThrows(RpcException.class, () -> {
            DemoService server = new DemoServiceImpl();

            URL url = this.registerProvider(exportUrl, server, DemoService.class);

            URL nettyUrl = url.addParameter(SERVER_KEY, "netty");
            Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));

            DemoService demoService = this.proxy.getProxy(protocol.refer(DemoService.class, nettyUrl));

            demoService.error();
        });
    }

    @Test
    public void testInvoke() {
        DemoService server = new DemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, DemoService.class);

        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(server, DemoService.class, url));

        RpcInvocation rpcInvocation = new RpcInvocation("hello", DemoService.class.getName(), "", new Class[]{Integer.class, Integer.class}, new Integer[]{2, 3});

        Result result = exporter.getInvoker().invoke(rpcInvocation);
        assertThat(result.getValue(), CoreMatchers.<Object>is(5));
    }

    @Test
    public void testFilter() {
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
    public void testRpcContextFilter() {
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

    @Test
    public void testRegFail() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            DemoService server = new DemoServiceImpl();

            URL url = this.registerProvider(exportUrl, server, DemoService.class);

            URL nettyUrl = url.addParameter(EXTENSION_KEY, "com.not.existing.Filter");
            protocol.export(proxy.getInvoker(server, DemoService.class, nettyUrl));
        });
    }

    @Test
    public void testDefaultPort() {
        assertThat(protocol.getDefaultPort(), is(80));
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
