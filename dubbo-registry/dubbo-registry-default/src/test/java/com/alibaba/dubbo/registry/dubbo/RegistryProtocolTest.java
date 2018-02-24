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
package com.alibaba.dubbo.registry.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.integration.RegistryProtocol;
import com.alibaba.dubbo.registry.support.AbstractRegistry;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.cluster.support.FailfastCluster;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboInvoker;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * RegistryProtocolTest
 *
 */
public class RegistryProtocolTest {

    static {
        SimpleRegistryExporter.exportIfAbsent(9090);
    }

    final String service = "com.alibaba.dubbo.registry.protocol.DemoService:1.0.0";
    final String serviceUrl = "dubbo://127.0.0.1:9453/" + service + "?notify=true&methods=test1,test2&side=con&side=consumer";
    final URL registryUrl = URL.valueOf("registry://127.0.0.1:9090/");
    final private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    @Test
    public void testDefaultPort() {
        RegistryProtocol registryProtocol = new RegistryProtocol();
        assertEquals(9090, registryProtocol.getDefaultPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportUrlNull() {
        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setCluster(new FailfastCluster());

        Protocol dubboProtocol = DubboProtocol.getDubboProtocol();
        registryProtocol.setProtocol(dubboProtocol);
        Invoker<DemoService> invoker = new DubboInvoker<DemoService>(DemoService.class,
                registryUrl, new ExchangeClient[]{new MockedClient("10.20.20.20", 2222, true)});
        registryProtocol.export(invoker);
    }

    @Test
    public void testExport() {
        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setCluster(new FailfastCluster());
        registryProtocol.setRegistryFactory(ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension());

        Protocol dubboProtocol = DubboProtocol.getDubboProtocol();
        registryProtocol.setProtocol(dubboProtocol);
        URL newRegistryUrl = registryUrl.addParameter(Constants.EXPORT_KEY, serviceUrl);
        DubboInvoker<DemoService> invoker = new DubboInvoker<DemoService>(DemoService.class,
                newRegistryUrl, new ExchangeClient[]{new MockedClient("10.20.20.20", 2222, true)});
        Exporter<DemoService> exporter = registryProtocol.export(invoker);
        Exporter<DemoService> exporter2 = registryProtocol.export(invoker);
        //The same invoker, exporter that multiple exported are different
        Assert.assertNotSame(exporter, exporter2);
        exporter.unexport();
        exporter2.unexport();

    }

    @Test
    public void testNotifyOverride() throws Exception {
        URL newRegistryUrl = registryUrl.addParameter(Constants.EXPORT_KEY, serviceUrl);
        Invoker<RegistryProtocolTest> invoker = new MockInvoker<RegistryProtocolTest>(RegistryProtocolTest.class, newRegistryUrl);
        Exporter<?> exporter = protocol.export(invoker);
        RegistryProtocol rprotocol = RegistryProtocol.getRegistryProtocol();
        NotifyListener listener = getListener(rprotocol);
        List<URL> urls = new ArrayList<URL>();
        urls.add(URL.valueOf("override://0.0.0.0/?timeout=1000"));
        urls.add(URL.valueOf("override://0.0.0.0/" + service + "?timeout=100"));
        urls.add(URL.valueOf("override://0.0.0.0/" + service + "?x=y"));
        listener.notify(urls);

        assertEquals(true, exporter.getInvoker().isAvailable());
        assertEquals("100", exporter.getInvoker().getUrl().getParameter("timeout"));
        assertEquals("y", exporter.getInvoker().getUrl().getParameter("x"));

        exporter.unexport();
//        int timeout = ConfigUtils.getServerShutdownTimeout();
//        Thread.sleep(timeout + 1000);
//        assertEquals(false, exporter.getInvoker().isAvailable());
        destroyRegistryProtocol();

    }


    /**
     * The name of the service does not match and can't override invoker
     * Service name matching, service version number mismatch
     */
    @Test
    public void testNotifyOverride_notmatch() throws Exception {
        URL newRegistryUrl = registryUrl.addParameter(Constants.EXPORT_KEY, serviceUrl);
        Invoker<RegistryProtocolTest> invoker = new MockInvoker<RegistryProtocolTest>(RegistryProtocolTest.class, newRegistryUrl);
        Exporter<?> exporter = protocol.export(invoker);
        RegistryProtocol rprotocol = RegistryProtocol.getRegistryProtocol();
        NotifyListener listener = getListener(rprotocol);
        List<URL> urls = new ArrayList<URL>();
        urls.add(URL.valueOf("override://0.0.0.0/com.alibaba.dubbo.registry.protocol.HackService?timeout=100"));
        listener.notify(urls);
        assertEquals(true, exporter.getInvoker().isAvailable());
        assertEquals(null, exporter.getInvoker().getUrl().getParameter("timeout"));
        exporter.unexport();
        destroyRegistryProtocol();
    }

    /**
     * Test destory registry, exporter can be normal by destroyed
     */
    @Test
    public void testDestoryRegistry() {
        URL newRegistryUrl = registryUrl.addParameter(Constants.EXPORT_KEY, serviceUrl);
        Invoker<RegistryProtocolTest> invoker = new MockInvoker<RegistryProtocolTest>(RegistryProtocolTest.class, newRegistryUrl);
        Exporter<?> exporter = protocol.export(invoker);
        destroyRegistryProtocol();
        assertEquals(false, exporter.getInvoker().isAvailable());

    }

    private void destroyRegistryProtocol() {
        Protocol registry = RegistryProtocol.getRegistryProtocol();
        registry.destroy();
    }

    private NotifyListener getListener(RegistryProtocol protocol) throws Exception {
        return protocol.getOverrideListeners().values().iterator().next();
    }

    static class MockInvoker<T> extends AbstractInvoker<T> {
        public MockInvoker(Class<T> type, URL url) {
            super(type, url);
        }

        @Override
        protected Result doInvoke(Invocation invocation) throws Throwable {
            //do nothing
            return null;
        }
    }

    static class MockRegistry extends AbstractRegistry {

        public MockRegistry(URL url) {
            super(url);
        }

        public boolean isAvailable() {
            return true;
        }
    }

}