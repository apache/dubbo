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
package org.apache.dubbo.rpc.protocol.nativethrift;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * ThriftProtocolTest
 */
public class ThriftProtocolTest {

    @Test
    public void testThriftProtocol() throws TException{
        org.apache.dubbo.rpc.protocol.nativethrift.DemoServiceImpl server = new org.apache.dubbo.rpc.protocol.nativethrift.DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(org.apache.dubbo.rpc.protocol.nativethrift.ThriftProtocol.NAME + "://127.0.0.1:5341/" + org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface.class.getName() + "?version=1.0.0&nthrift.overload.method=true");
        Exporter<org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface.class, url));
        Invoker<org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface> invoker = protocol.refer(org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface.class, url);
        org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testThriftProtocolMultipleServices() throws TException{

        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        org.apache.dubbo.rpc.protocol.nativethrift.DemoServiceImpl server1 = new org.apache.dubbo.rpc.protocol.nativethrift.DemoServiceImpl();
        URL url1 = URL.valueOf(org.apache.dubbo.rpc.protocol.nativethrift.ThriftProtocol.NAME + "://127.0.0.1:5342/" + org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface.class.getName() + "?version=1.0.0&nthrift.overload.method=true");
        Exporter<org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface> exporter1 = protocol.export(proxyFactory.getInvoker(server1, org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface.class, url1));
        Invoker<org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface> invoker1 = protocol.refer(org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface.class, url1);
        org.apache.dubbo.rpc.protocol.nativethrift.DemoService.Iface client1 = proxyFactory.getProxy(invoker1);
        String result1 = client1.sayHello("haha");
        Assertions.assertTrue(server1.isCalled());
        Assertions.assertEquals("Hello, haha", result1);

        org.apache.dubbo.rpc.protocol.nativethrift.UserServiceImpl server2 = new org.apache.dubbo.rpc.protocol.nativethrift.UserServiceImpl();
        URL url2 = URL.valueOf(org.apache.dubbo.rpc.protocol.nativethrift.ThriftProtocol.NAME + "://127.0.0.1:5342/" + org.apache.dubbo.rpc.protocol.nativethrift.UserService.Iface.class.getName() + "?version=1.0.0&nthrift.overload.method=true");
        Exporter<org.apache.dubbo.rpc.protocol.nativethrift.UserService.Iface> exporter2 = protocol.export(proxyFactory.getInvoker(server2, org.apache.dubbo.rpc.protocol.nativethrift.UserService.Iface.class, url2));
        Invoker<org.apache.dubbo.rpc.protocol.nativethrift.UserService.Iface> invoker2 = protocol.refer(org.apache.dubbo.rpc.protocol.nativethrift.UserService.Iface.class, url2);
        org.apache.dubbo.rpc.protocol.nativethrift.UserService.Iface client2 = proxyFactory.getProxy(invoker2);
        String result2 = client2.find(2);
        Assertions.assertEquals("KK2", result2);

        invoker1.destroy();
        exporter1.unexport();
        invoker2.destroy();
        exporter2.unexport();
    }

}
