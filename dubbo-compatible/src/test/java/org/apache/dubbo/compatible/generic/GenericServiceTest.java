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

package org.apache.dubbo.compatible.generic;

import com.alibaba.dubbo.rpc.service.GenericService;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.compatible.service.DemoService;
import org.apache.dubbo.compatible.service.DemoServiceImpl;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.junit.Assert;
import org.junit.Test;

public class GenericServiceTest {

    @Test
    public void testGeneric() {
        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));
        Invoker<DemoService> invoker = protocol.refer(DemoService.class, url);

        GenericService client = (GenericService) proxyFactory.getProxy(invoker, true);
        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assert.assertEquals("hello haha", result);

        org.apache.dubbo.rpc.service.GenericService newClient = (org.apache.dubbo.rpc.service.GenericService) proxyFactory.getProxy(invoker, true);
        Object res = newClient.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"hehe"});
        Assert.assertEquals("hello hehe", res);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testGeneric2() {
        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0&generic=true");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);

        GenericService client = proxyFactory.getProxy(invoker, true);
        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assert.assertEquals("hello haha", result);

        Invoker<DemoService> invoker2 = protocol.refer(DemoService.class, url);

        GenericService client2 = (GenericService) proxyFactory.getProxy(invoker2, true);
        Object result2 = client2.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assert.assertEquals("hello haha", result2);

        invoker.destroy();
        exporter.unexport();
    }
}
