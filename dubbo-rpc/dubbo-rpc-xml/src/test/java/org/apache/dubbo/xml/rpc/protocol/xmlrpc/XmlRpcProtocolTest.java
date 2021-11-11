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
package org.apache.dubbo.xml.rpc.protocol.xmlrpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


public class XmlRpcProtocolTest {

    @Test
    public void testXmlRpcProtocol() {
        XmlRpcServiceImpl server = new XmlRpcServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("xmlrpc://127.0.0.1:" + port + "/" + XmlRpcService.class.getName() + "?version=1.0.0");
        Exporter<XmlRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, XmlRpcService.class, url));
        Invoker<XmlRpcService> invoker = protocol.refer(XmlRpcService.class, url);
        XmlRpcService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testXmlRpcProtocolForServerJetty9() {
        XmlRpcServiceImpl server = new XmlRpcServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("xmlrpc://127.0.0.1:" + port + "/" + XmlRpcService.class.getName() + "?version=1.0.0&server=jetty9");
        Exporter<XmlRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, XmlRpcService.class, url));
        Invoker<XmlRpcService> invoker = protocol.refer(XmlRpcService.class, url);
        XmlRpcService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    @Disabled
    public void testCustomException() {
        XmlRpcServiceImpl server = new XmlRpcServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("xmlrpc://127.0.0.1:" + port + "/" +
                XmlRpcService.class.getName() + "?version=1.0.0&server=jetty9");
        Exporter<XmlRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, XmlRpcService.class, url));
        Invoker<XmlRpcService> invoker = protocol.refer(XmlRpcService.class, url);
        XmlRpcService client = proxyFactory.getProxy(invoker);
        try {
            client.customException();
            Assertions.fail();
        } catch (XmlRpcServiceImpl.MyException expected) {
        }
        invoker.destroy();
        exporter.unexport();
    }

}