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

package org.apache.dubbo.echo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.service.DemoService;
import org.apache.dubbo.service.DemoServiceImpl;

import com.alibaba.dubbo.rpc.service.EchoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EchoServiceTest {

    @Test
    public void testEcho() {
        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));
        Invoker<DemoService> invoker = protocol.refer(DemoService.class, url);
        EchoService client = (EchoService) proxyFactory.getProxy(invoker);
        Object result = client.$echo("haha");
        Assertions.assertEquals("haha", result);

        org.apache.dubbo.rpc.service.EchoService newClient = (org.apache.dubbo.rpc.service.EchoService) proxyFactory.getProxy(invoker);
        Object res = newClient.$echo("hehe");
        Assertions.assertEquals("hehe", res);
        invoker.destroy();
        exporter.unexport();
    }
}
