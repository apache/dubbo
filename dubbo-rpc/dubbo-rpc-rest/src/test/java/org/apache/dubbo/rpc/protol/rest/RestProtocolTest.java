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
package org.apache.dubbo.rpc.protol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * RestProtocolTest
 */
public class RestProtocolTest {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testRestProtocol() {
        URL url = URL.valueOf("rest://127.0.0.1:5342/rest/say1?version=1.0.0");
        RestServiceImpl server = new RestServiceImpl();
        ProviderModel providerModel = new ProviderModel(url.getServiceKey(), server, RestService.class);
        ApplicationModel.initProviderModel(url.getServiceKey(), providerModel);

        Exporter<RestService> exporter = protocol.export(proxyFactory.getInvoker(server, RestService.class, url));
        Invoker<RestService> invoker = protocol.refer(RestService.class, url);        Assertions.assertFalse(server.isCalled());

        RestService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testRestProtocolWithContextPath() {
        RestServiceImpl server = new RestServiceImpl();
        Assertions.assertFalse(server.isCalled());
        URL url = URL.valueOf("rest://127.0.0.1:5341/a/b/c?version=1.0.0");
        ProviderModel providerModel = new ProviderModel(url.getServiceKey(), server, RestService.class);
        ApplicationModel.initProviderModel(url.getServiceKey(), providerModel);

        Exporter<RestService> exporter = protocol.export(proxyFactory.getInvoker(server, RestService.class, url));

        url = URL.valueOf("rest://127.0.0.1:5341/a/b/c/?version=1.0.0");
        Invoker<RestService> invoker = protocol.refer(RestService.class, url);
        RestService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }
}
