/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.injvm;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * <code>ProxiesTest</code>
 */

public class InjvmProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();

    @After
    public void after() throws Exception {
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        exporters.clear();
    }

    @Test
    public void testLocalProtocol() throws Exception {
        DemoService service = new DemoServiceImpl();
        Exporter<?> exporter = protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService").addParameter(Constants.INTERFACE_KEY, DemoService.class.getName())));
        exporters.add(exporter);
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService").addParameter(Constants.INTERFACE_KEY, DemoService.class.getName())));
        assertEquals(service.getSize(new String[]{"", "", ""}), 3);
        service.invoke("injvm://127.0.0.1/TestService", "invoke");
    }

    @Test
    public void testIsInjvmRefer() throws Exception {
        DemoService service = new DemoServiceImpl();
        URL url = URL.valueOf("injvm://127.0.0.1/TestService")
                .addParameter(Constants.INTERFACE_KEY, DemoService.class.getName());
        Exporter<?> exporter = protocol.export(proxy.getInvoker(service, DemoService.class, url));
        exporters.add(exporter);

        url = url.setProtocol("dubbo");
        assertTrue(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));

        url = url.addParameter(Constants.GROUP_KEY, "*")
                .addParameter(Constants.VERSION_KEY, "*");
        assertTrue(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));
    }

}