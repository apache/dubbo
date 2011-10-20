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
package com.alibaba.dubbo.registry.protocol;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.MockedClient;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.support.SimpleRegistryExporter;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.cluster.support.FailfastCluster;
import com.alibaba.dubbo.rpc.dubbo.DubboInvoker;
import com.alibaba.dubbo.rpc.dubbo.DubboProtocol;

/**
 * RegistryProtocolTest
 * 
 * @author tony.chenl
 */
public class RegistryProtocolTest {

    static {
        SimpleRegistryExporter.exportIfAbsent(9090);
    }

    String service     = "com.alibaba.dubbo.registry.protocol.DemoService:1.0.0";
    String serviceUrl  = "dubbo://127.0.0.1:9453/" + service + "?notify=true&methods=test1,test2";
    URL    registryUrl = URL.valueOf("dubbo://127.0.0.1:9090/");

    @Test
    public void testDefaultPort() {
        RegistryProtocol registryProtocol = new RegistryProtocol();
        assertEquals(9090, registryProtocol.getDefaultPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportUrlNull() {
        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setCluster(new FailfastCluster());

        Protocol dubboProtocol = new DubboProtocol();
        registryProtocol.setProtocol(dubboProtocol);
        Invoker<DemoService> invoker = new DubboInvoker<DemoService>(DemoService.class,
                registryUrl, new ExchangeClient[] { new MockedClient("10.20.20.20", 2222, true) });
        registryProtocol.export(invoker);
    }

    @Test
    public void testExport() {
        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setCluster(new FailfastCluster());
        registryProtocol.setRegistryFactory(ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension());

        Protocol dubboProtocol = new DubboProtocol();
        registryProtocol.setProtocol(dubboProtocol);
        registryUrl = registryUrl.addParameter(RpcConstants.EXPORT_KEY, serviceUrl);
        DubboInvoker<DemoService> invoker = new DubboInvoker<DemoService>(DemoService.class,
                registryUrl, new ExchangeClient[] { new MockedClient("10.20.20.20", 2222, true) });
        Exporter<DemoService> exporter = registryProtocol.export(invoker);
        assertEquals(invoker, exporter.getInvoker());
        exporter.unexport();
    }

}