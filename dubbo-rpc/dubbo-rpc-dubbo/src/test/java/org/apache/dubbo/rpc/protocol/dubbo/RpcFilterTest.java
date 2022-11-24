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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;
import org.apache.dubbo.rpc.service.EchoService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RpcFilterTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @AfterEach
    public void after() {
        ProtocolUtils.closeAll();
    }

    @Test
    void testRpcFilter() throws Exception {
        DemoService service = new DemoServiceImpl();
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/org.apache.dubbo.rpc.protocol.dubbo.support.DemoService?service.filter=echo");
        ApplicationModel.defaultModel().getDefaultModule().getServiceRepository().registerService(DemoService.class);
        url = url.setScopeModel(ApplicationModel.defaultModel().getDefaultModule());
        protocol.export(proxy.getInvoker(service, DemoService.class, url));
        service = proxy.getProxy(protocol.refer(DemoService.class, url));
        Assertions.assertEquals("123", service.echo("123"));
        // cast to EchoService
        EchoService echo = proxy.getProxy(protocol.refer(EchoService.class, url));
        Assertions.assertEquals(echo.$echo("test"), "test");
        Assertions.assertEquals(echo.$echo("abcdefg"), "abcdefg");
        Assertions.assertEquals(echo.$echo(1234), 1234);
    }

}
