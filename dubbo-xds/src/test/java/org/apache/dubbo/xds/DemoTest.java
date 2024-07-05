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
package org.apache.dubbo.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.auth.DemoService;
import org.apache.dubbo.xds.directory.XdsDirectory;
import org.apache.dubbo.xds.resource.XdsCluster;
import org.apache.dubbo.xds.resource.XdsVirtualHost;
import org.apache.dubbo.xds.router.XdsRouter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

public class DemoTest {

    //    private Protocol protocol =
    //            ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    //
    //    private ProxyFactory proxy =
    //            ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    //

    //    @Test
    public void testXdsRouterInitial() throws InterruptedException {
        System.setProperty("API_SERVER_PATH", "https://127.0.0.1:6443");
        System.setProperty("SA_CA_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
        System.setProperty(
                "SA_TOKEN_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo");
        System.setProperty("NAMESPACE", "foo");

        System.setProperty("CA_ADDR_KEY", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");

        //        ApplicationModel app = FrameworkModel.defaultModel().defaultApplication();
        //        KubeEnv kubeEnv = new KubeEnv(app);
        //        kubeEnv.setNamespace("foo");
        //        kubeEnv.setEnableSsl(true);
        //        kubeEnv.setApiServerPath( "https://127.0.0.1:6443");
        //
        // kubeEnv.setServiceAccountTokenPath("/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo");
        //
        // kubeEnv.setServiceAccountCaPath("/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
        //        app.getBeanFactory().registerBean(kubeEnv);

        URL url = URL.valueOf("xds://localhost:15010/?secure=plaintext");

        PilotExchanger.initialize(url);

        new CountDownLatch(1).await();

        Thread.sleep(7000);

        Directory directory = Mockito.mock(Directory.class);
        Mockito.when(directory.getConsumerUrl())
                .thenReturn(URL.valueOf("dubbo://0.0.0.0:15010/DemoService?provided-by=dubbo-samples-xds-provider"));
        Mockito.when(directory.getInterface()).thenReturn(DemoService.class);
        // doReturn(DemoService.class).when(directory.getInterface());
        //        Mockito.when(directory.getProtocol()).thenReturn(protocol);

        SingleRouterChain singleRouterChain =
                new SingleRouterChain<>(Collections.emptyList(), Arrays.asList(new XdsRouter<>(url)), false, null);
        RouterChain routerChain = new RouterChain<>(new SingleRouterChain[] {singleRouterChain, singleRouterChain});
        // doReturn(routerChain).when(directory.getRouterChain());
        Mockito.when(directory.getRouterChain()).thenReturn(routerChain);

        XdsDirectory<?> xdsDirectory = new XdsDirectory<>(directory);

        Invocation invocation = Mockito.mock(Invocation.class);
        Invoker invoker = Mockito.mock(Invoker.class);
        URL url1 = URL.valueOf("consumer://0.0.0.0:15010/DemoService?providedBy=dubbo-samples-xds-provider&xds=true");
        Mockito.when(invoker.getUrl()).thenReturn(url1);
        // doReturn(invoker).when(invocation.getInvoker());
        Mockito.when(invocation.getInvoker()).thenReturn(invoker);

        while (true) {
            Map<String, XdsVirtualHost> xdsVirtualHostMap = xdsDirectory.getXdsVirtualHostMap();
            Map<String, ? extends XdsCluster<?>> xdsClusterMap = xdsDirectory.getXdsClusterMap();
            if (!xdsVirtualHostMap.isEmpty() && !xdsClusterMap.isEmpty()) {
                // xdsRouterDemo.route(invokers, url, invocation, false, null);
                xdsDirectory.list(invocation);
                break;
            }
            Thread.yield();
        }
    }

    private Invoker<Object> createInvoker(String app, String address) {
        URL url = URL.valueOf("dubbo://" + address + "/DemoInterface?"
                + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    @AfterAll
    public static void after() {
        //        ProtocolUtils.closeAll();
        ApplicationModel.defaultModel()
                .getDefaultModule()
                .getServiceRepository()
                .unregisterService(DemoService.class);
    }

    @BeforeAll
    public static void setup() {
        ApplicationModel.defaultModel()
                .getDefaultModule()
                .getServiceRepository()
                .registerService(DemoService.class);
    }
}
