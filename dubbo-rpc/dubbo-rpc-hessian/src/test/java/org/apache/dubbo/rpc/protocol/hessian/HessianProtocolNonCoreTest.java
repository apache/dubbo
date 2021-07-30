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
package org.apache.dubbo.rpc.protocol.hessian;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.hessian.HessianServiceImpl.MyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * HessianProtocolTest
 * On some machines, there is a limit on the maximum number of threads.
 * Therefore, the test cases of the Hessian protocol are split into two files
 */
public class HessianProtocolNonCoreTest {

    @Test
    public void testOverload() {
        HessianServiceImpl server = new HessianServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("hessian://127.0.0.1:" + port + "/" + HessianService.class.getName() + "?version=1.0.0&hessian.overload.method=true&hessian2.request=false");
        Exporter<HessianService> exporter = protocol.export(proxyFactory.getInvoker(server, HessianService.class, url));
        Invoker<HessianService> invoker = protocol.refer(HessianService.class, url);
        HessianService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertEquals("Hello, haha", result);
        result = client.sayHello("haha", 1);
        Assertions.assertEquals("Hello, haha. ", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testHttpClient() {
        HessianServiceImpl server = new HessianServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("hessian://127.0.0.1:" + port + "/" + HessianService.class.getName() + "?version=1.0.0&client=httpclient&hessian.overload.method=true");
        Exporter<HessianService> exporter = protocol.export(proxyFactory.getInvoker(server, HessianService.class, url));
        Invoker<HessianService> invoker = protocol.refer(HessianService.class, url);
        HessianService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testTimeOut() {
        HessianServiceImpl server = new HessianServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("hessian://127.0.0.1:" + port + "/" + HessianService.class.getName() + "?version=1.0.0&timeout=10");
        Exporter<HessianService> exporter = protocol.export(proxyFactory.getInvoker(server, HessianService.class, url));
        Invoker<HessianService> invoker = protocol.refer(HessianService.class, url);
        HessianService client = proxyFactory.getProxy(invoker);
        try {
            client.timeOut(6000);
            fail();
        } catch (RpcException expected) {
            Assertions.assertTrue(expected.isTimeout());
        } finally {
            invoker.destroy();
            exporter.unexport();
        }

    }

    @Test
    public void testCustomException() {
        HessianServiceImpl server = new HessianServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("hessian://127.0.0.1:" + port + "/" + HessianService.class.getName() + "?version=1.0.0");
        Exporter<HessianService> exporter = protocol.export(proxyFactory.getInvoker(server, HessianService.class, url));
        Invoker<HessianService> invoker = protocol.refer(HessianService.class, url);
        HessianService client = proxyFactory.getProxy(invoker);
        try {
            client.customException();
            fail();
        } catch (MyException expected) {

        }
        invoker.destroy();
        exporter.unexport();
    }


    @Test
    public void testRemoteApplicationName() {
        HessianServiceImpl server = new HessianServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("hessian://127.0.0.1:" + port + "/" + HessianService.class.getName() + "?version=1.0.0&hessian.overload.method=true").addParameter("application", "consumer");
        Exporter<HessianService> exporter = protocol.export(proxyFactory.getInvoker(server, HessianService.class, url));
        Invoker<HessianService> invoker = protocol.refer(HessianService.class, url);
        HessianService client = proxyFactory.getProxy(invoker);
        String result = client.getRemoteApplicationName();
        Assertions.assertEquals("consumer", result);
        invoker.destroy();
        exporter.unexport();
    }

}
