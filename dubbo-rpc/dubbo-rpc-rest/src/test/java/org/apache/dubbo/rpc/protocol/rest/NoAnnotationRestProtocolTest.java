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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.rest.noannotation.NoAnnotationDemoService;
import org.apache.dubbo.rpc.protocol.rest.noannotation.NoAnnotationDemoServiceImpl;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoAnnotationRestProtocolTest {
    private final Protocol protocol =
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private final ProxyFactory proxy =
            ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private final ModuleServiceRepository repository =
            ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    @AfterEach
    public void tearDown() {
        protocol.destroy();
        FrameworkModel.destroyAll();
        new JsonUtils() {
            public void clearJson() {
                setJson(null);
            }
        }.clearJson();
        SystemPropertyConfigUtils.clearSystemProperty(CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME);
    }

    @Test
    void testJson() {
        List<String> jsons = Arrays.asList("fastjson", "fastjson2", "jackson", "gson");
        for (String json : jsons) {
            new JsonUtils() {
                public void clearJson() {
                    setJson(null);
                }
            }.clearJson();
            SystemPropertyConfigUtils.setSystemProperty(
                    CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME, json);
            testRestProtocol();
        }
    }

    void testRestProtocol() {
        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort()
                + "/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.noannotation.NoAnnotationDemoService");

        NoAnnotationDemoServiceImpl server = new NoAnnotationDemoServiceImpl();

        url = this.registerProvider(url, server, DemoService.class);

        Exporter<NoAnnotationDemoService> exporter =
                protocol.export(proxy.getInvoker(server, NoAnnotationDemoService.class, url));
        Invoker<NoAnnotationDemoService> invoker = protocol.refer(NoAnnotationDemoService.class, url);

        NoAnnotationDemoService client = proxy.getProxy(invoker);
        Object result = client.sayHello("haha");
        Assertions.assertEquals("Hello, haha", result);

        result = client.hello(1, 2);
        Assertions.assertEquals(3, result);

        User user = client.user(User.getInstance());
        Assertions.assertEquals("invoked", user.getName());

        Assertions.assertEquals(
                User.getInstance(),
                client.userList(Arrays.asList(User.getInstance())).get(0));

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
                + "/?version=1.0.0&interface=org.apache.dubbo.rpc.protocol.rest.noannotation.NoAnnotationDemoService&"
                + org.apache.dubbo.remoting.Constants.CLIENT_KEY + "=" + restClient);

        NoAnnotationDemoService server = new NoAnnotationDemoServiceImpl();

        url = this.registerProvider(url, server, DemoService.class);

        Exporter<NoAnnotationDemoService> exporter =
                protocol.export(proxy.getInvoker(server, NoAnnotationDemoService.class, url));
        Invoker<NoAnnotationDemoService> invoker = protocol.refer(NoAnnotationDemoService.class, url);

        NoAnnotationDemoService client = proxy.getProxy(invoker);
        String name = "no-annotation";
        String result = client.sayHello(name);

        Assertions.assertEquals("Hello, " + name, result);

        invoker.destroy();
        exporter.unexport();
    }

    private URL registerProvider(URL url, Object impl, Class<?> interfaceClass) {
        ServiceDescriptor serviceDescriptor = repository.registerService(interfaceClass);
        ProviderModel providerModel = new ProviderModel(url.getServiceKey(), impl, serviceDescriptor, null, null);
        repository.registerProvider(providerModel);
        return url.setServiceModel(providerModel);
    }
}
