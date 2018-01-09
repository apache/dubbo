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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;

import junit.framework.TestCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadTest extends TestCase {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    public void testDubboMultiThreadInvoke() throws Exception {
        Exporter<?> rpcExporter = protocol.export(proxy.getInvoker(new DemoServiceImpl(), DemoService.class, URL.valueOf("dubbo://127.0.0.1:20259/TestService")));

        final AtomicInteger counter = new AtomicInteger();
        final DemoService service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("dubbo://127.0.0.1:20259/TestService")));
        assertEquals(service.getSize(new String[]{"123", "456", "789"}), 3);

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1024 * 64 + 32; i++)
            sb.append('A');
        assertEquals(sb.toString(), service.echo(sb.toString()));

        ExecutorService exec = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            final int fi = i;
            exec.execute(new Runnable() {
                public void run() {
                    for (int i = 0; i < 30; i++) {
                        System.out.println(fi + ":" + counter.getAndIncrement());
                        assertEquals(service.echo(sb.toString()), sb.toString());
                    }
                }
            });
        }
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        rpcExporter.unexport();
    }
}