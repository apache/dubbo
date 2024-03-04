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
package org.apache.dubbo.rpc.cluster.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class DemoTest {

    private Protocol protocol =
            ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private ProxyFactory proxy =
            ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testXdsRouterInitial() {

        // URL url = URL.valueOf("xds://localhost:15010/?secure=plaintext");
        //
        // PilotExchanger.initialize(url);
        //
        // Directory directory = Mockito.spy(Directory.class);
        // when(directory.getConsumerUrl())
        //         .thenReturn(URL.valueOf("dubbo://0.0.0.0:15010/DemoService?providedBy=dubbo-samples-xds-provider"));
        // when(directory.getInterface()).thenReturn(DemoService.class);
        // when(directory.getProtocol()).thenReturn(protocol);
        // SingleRouterChain singleRouterChain =
        //         new SingleRouterChain<>(null, Arrays.asList(new XdsRouter<>(url)), false, null);
        // RouterChain routerChain = new RouterChain<>(new SingleRouterChain[] {singleRouterChain});
        // when(directory.getRouterChain()).thenReturn(routerChain);
        //
        // XdsDirectory xdsDirectory = new XdsDirectory(directory);
        //
        // Invocation invocation = Mockito.mock(Invocation.class);
        // Invoker invoker = Mockito.mock(Invoker.class);
        // URL url1 = URL.valueOf("consumer://0.0.0.0:15010/DemoService?providedBy=dubbo-samples-xds-provider");
        // when(invoker.getUrl()).thenReturn(url1);
        // when(invocation.getInvoker()).thenReturn(invoker);
        //
        // while (true) {
        //     Map<String, XdsVirtualHost> xdsVirtualHostMap = xdsDirectory.getXdsVirtualHostMap();
        //     Map<String, XdsCluster> xdsClusterMap = xdsDirectory.getXdsClusterMap();
        //     if (!xdsVirtualHostMap.isEmpty() && !xdsClusterMap.isEmpty()) {
        //         // xdsRouterDemo.route(invokers, url, invocation, false, null);
        //         xdsDirectory.list(invocation);
        //     }
        //     Thread.yield();
        // }
    }

    private Invoker<Object> createInvoker(String app, String address) {
        URL url = URL.valueOf("dubbo://" + address + "/DemoInterface?"
                + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }
}
