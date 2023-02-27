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
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class MultiThreadTest {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @AfterEach
    public void after() {
        ProtocolUtils.closeAll();
        ApplicationModel.defaultModel().getDefaultModule().getServiceRepository().destroy();
    }

    @Test
    void testDubboMultiThreadInvoke() throws Exception {
        ApplicationModel.defaultModel().getDefaultModule().getServiceRepository().registerService("TestService", DemoService.class);
        int port = NetUtils.getAvailablePort();
        Exporter<?> rpcExporter = protocol.export(proxy.getInvoker(new DemoServiceImpl(), DemoService.class, URL.valueOf("dubbo://127.0.0.1:" + port + "/TestService")));

        final AtomicInteger counter = new AtomicInteger();
        final DemoService service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("dubbo://127.0.0.1:" + port + "/TestService")));
        Assertions.assertEquals(service.getSize(new String[]{"123", "456", "789"}), 3);

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1024 * 64 + 32; i++)
            sb.append('A');
        Assertions.assertEquals(sb.toString(), service.echo(sb.toString()));

        ExecutorService exec = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            final int fi = i;
            exec.execute(new Runnable() {
                public void run() {
                    for (int i = 0; i < 30; i++) {
                        System.out.println(fi + ":" + counter.getAndIncrement());
                        Assertions.assertEquals(service.echo(sb.toString()), sb.toString());
                    }
                }
            });
        }
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        rpcExporter.unexport();
    }
}
