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
package org.apache.dubbo.rpc.protocol.webservice;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 *
 */

public class WebserviceProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testDemoProtocol() throws Exception {
        DemoService service = new DemoServiceImpl();
        Exporter<DemoService> exporter = protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("webservice://127.0.0.1:9019/" + DemoService.class.getName() + "?codec=exchange")));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("webservice://127.0.0.1:9019/" + DemoService.class.getName() + "?codec=exchange&timeout=3000")));
        assertEquals(service.getSize(new String[]{"", "", ""}), 3);
        exporter.unexport();
    }

    @Test
    public void testWebserviceProtocol() throws Exception {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("webservice://127.0.0.1:9019/" + DemoService.class.getName())));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("webservice://127.0.0.1:9019/" + DemoService.class.getName() + "?timeout=3000")));
        assertEquals(service.create(1, "kk").getName(), "kk");
        assertEquals(service.getSize(null), -1);
        assertEquals(service.getSize(new String[]{"", "", ""}), 3);
        Object object = service.invoke("webservice://127.0.0.1:9019/" + DemoService.class.getName() + "", "invoke");
        System.out.println(object);
        assertEquals("webservice://127.0.0.1:9019/org.apache.dubbo.rpc.protocol.webservice.DemoService:invoke", object);

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 1024 * 32 + 32; i++)
            buf.append('A');
        assertEquals(32800, service.stringLength(buf.toString()));

//  a method start with $ is illegal in soap
//        // cast to EchoService
//        EchoService echo = proxy.getProxy(protocol.refer(EchoService.class, URL.valueOf("webservice://127.0.0.1:9010/" + DemoService.class.getName() + "?client=netty")));
//        assertEquals(echo.echo(buf.toString()), buf.toString());
//        assertEquals(echo.$echo("test"), "test");
//        assertEquals(echo.$echo("abcdefg"), "abcdefg");
//        assertEquals(echo.$echo(1234), 1234);
    }


}
