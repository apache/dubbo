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
package org.apache.dubbo.rpc.protocol.http;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpProtocolTest {

    @Test
    public void testJsonrpcProtocol() {
        HttpServiceImpl server = new HttpServiceImpl();
        assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HttpService.class.getName() + "?version=1.0.0");
        Exporter<HttpService> exporter = protocol.export(proxyFactory.getInvoker(server, HttpService.class, url));
        Invoker<HttpService> invoker = protocol.refer(HttpService.class, url);
        HttpService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        assertTrue(server.isCalled());
        assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testJsonrpcProtocolForServerJetty() {
        HttpServiceImpl server = new HttpServiceImpl();
        assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HttpService.class.getName() + "?version=1.0.0&server=jetty");
        Exporter<HttpService> exporter = protocol.export(proxyFactory.getInvoker(server, HttpService.class, url));
        Invoker<HttpService> invoker = protocol.refer(HttpService.class, url);
        HttpService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        assertTrue(server.isCalled());
        assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

}