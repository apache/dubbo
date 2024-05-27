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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring.compatible;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.util.LinkedMultiValueMap;

import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Disabled
public class SpringMvcRestProtocolTest {
    private final Protocol tProtocol =
            ApplicationModel.defaultModel().getExtensionLoader(Protocol.class).getExtension("tri");
    private final Protocol protocol =
            ApplicationModel.defaultModel().getExtensionLoader(Protocol.class).getExtension("rest");
    private final ProxyFactory proxy = ApplicationModel.defaultModel()
            .getExtensionLoader(ProxyFactory.class)
            .getAdaptiveExtension();

    private static URL getUrl() {
        return URL.valueOf("tri://127.0.0.1:" + NetUtils.getAvailablePort() + "/rest?interface="
                + SpringRestDemoService.class.getName());
    }

    private static final String SERVER = "netty4";

    private final ModuleServiceRepository repository =
            ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    @AfterEach
    public void tearDown() {
        tProtocol.destroy();
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
        url = url.addParameter(SERVER_KEY, SERVER);
        return tProtocol.export(proxy.getInvoker(server, getServerClass(), url));
    }

    @Test
    void testRestProtocol() {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf(
                "tri://127.0.0.1:" + port + "/?version=1.0.0&interface=" + SpringRestDemoService.class.getName());

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
    void testRestProtocolWithContextPath() {
        SpringRestDemoService server = getServerImpl();
        Assertions.assertFalse(server.isCalled());
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf(
                "tri://127.0.0.1:" + port + "/a/b/c?version=1.0.0&interface=" + SpringRestDemoService.class.getName());

        url = this.registerProvider(url, server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExport(url, server);

        url = URL.valueOf("rest://127.0.0.1:" + port + "/a/b/c/?version=1.0.0&interface="
                + SpringRestDemoService.class.getName());
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

        URL url = this.registerProvider(getUrl(), server, SpringRestDemoService.class);

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

        URL nettyUrl = this.registerProvider(getUrl(), server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, nettyUrl));

        Integer echoString = demoService.hello(10, 10);
        assertThat(echoString, is(20));

        exporter.unexport();
    }

    @Test
    void testInvoke() {
        SpringRestDemoService server = getServerImpl();

        URL url = this.registerProvider(getUrl(), server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExport(url, server);

        RpcInvocation rpcInvocation = new RpcInvocation(
                "hello",
                SpringRestDemoService.class.getName(),
                "",
                new Class[] {Integer.class, Integer.class},
                new Integer[] {2, 3});

        Result result = exporter.getInvoker().invoke(rpcInvocation);
        assertThat(result.getValue(), CoreMatchers.<Object>is(5));
    }

    @Test
    void testFilter() {
        SpringRestDemoService server = getServerImpl();

        URL url = this.registerProvider(getUrl(), server, SpringRestDemoService.class);

        Exporter<SpringRestDemoService> exporter = getExport(url, server);

        SpringRestDemoService demoService = this.proxy.getProxy(protocol.refer(SpringRestDemoService.class, url));

        Integer result = demoService.hello(1, 2);

        assertThat(result, is(3));

        exporter.unexport();
    }

    @Test
    void testDefaultPort() {
        assertThat(protocol.getDefaultPort(), is(80));
    }

    @Test
    void testFormConsumerParser() {
        SpringRestDemoService server = getServerImpl();

        URL nettyUrl = this.registerProvider(getUrl(), server, SpringRestDemoService.class);

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

        URL nettyUrl = this.registerProvider(getUrl(), server, SpringRestDemoService.class);

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

    @Test
    void testExceptionHandler() {
        SpringRestDemoService server = getServerImpl();

        URL nettyUrl = registerProvider(getUrl(), server, SpringRestDemoService.class);
        Exporter<SpringRestDemoService> exporter = getExport(nettyUrl, server);
        SpringRestDemoService demoService = proxy.getProxy(protocol.refer(SpringRestDemoService.class, nettyUrl));

        String result = demoService.error();

        assertThat(result, is("ok"));

        exporter.unexport();
    }

    @Test
    void testProxyDoubleCheck() {

        ProxyCreatorSupport proxyCreatorSupport = new ProxyCreatorSupport();
        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTarget(getServerImpl());
        AopProxy aopProxy = proxyCreatorSupport.getAopProxyFactory().createAopProxy(advisedSupport);
        Object proxy = aopProxy.getProxy();
        SpringRestDemoService server = (SpringRestDemoService) proxy;

        URL nettyUrl = this.registerProvider(getUrl(), server, SpringRestDemoService.class);

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

    private URL registerProvider(URL url, Object impl, Class<?> interfaceClass) {
        ServiceDescriptor serviceDescriptor = repository.registerService(interfaceClass);
        ProviderModel providerModel = new ProviderModel(url.getServiceKey(), impl, serviceDescriptor, null, null);
        repository.registerProvider(providerModel);
        return url.setServiceModel(providerModel);
    }
}
