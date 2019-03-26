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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;
import org.apache.dubbo.rpc.service.EchoService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class RpcFilterTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @After
    public void after() {
        ProtocolUtils.closeAll();
    }

    @Test
    public void testRpcFilter() throws Exception {
        DemoService service = new DemoServiceImpl();
        URL url = URL.valueOf("dubbo://127.0.0.1:9010/org.apache.dubbo.rpc.DemoService?service.filter=echo");
        protocol.export(proxy.getInvoker(service, DemoService.class, url));
        service = proxy.getProxy(protocol.refer(DemoService.class, url));
        Assert.assertEquals("123", service.echo("123"));
        // cast to EchoService
        EchoService echo = proxy.getProxy(protocol.refer(EchoService.class, url));
        Assert.assertEquals(echo.$echo("test"), "test");
        Assert.assertEquals(echo.$echo("abcdefg"), "abcdefg");
        Assert.assertEquals(echo.$echo(1234), 1234);
    }

}