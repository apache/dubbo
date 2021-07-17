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
package org.apache.dubbo.rpc.protocol.injvm;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

import org.apache.dubbo.rpc.protocol.DelegateExporterMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <code>ProxiesTest</code>
 */

public class InjvmProtocolTest {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();

    @AfterEach
    public void after() throws Exception {
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        exporters.clear();
    }

    @Test
    public void testLocalProtocol() throws Exception {
        DemoService service = new DemoServiceImpl();
        Invoker<?> invoker = proxy.getInvoker(service, DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService").addParameter(INTERFACE_KEY, DemoService.class.getName()));
        assertTrue(invoker.isAvailable());
        Exporter<?> exporter = protocol.export(invoker);
        exporters.add(exporter);
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService").addParameter(INTERFACE_KEY, DemoService.class.getName())));
        assertEquals(service.getSize(new String[]{"", "", ""}), 3);
        service.invoke("injvm://127.0.0.1/TestService", "invoke");

        InjvmInvoker injvmInvoker = new InjvmInvoker(DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService"), null, new DelegateExporterMap() {
            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public Exporter<?> getExport(String key) {
                return null;
            }

            @Override
            public void addExportMap(String key, Exporter<?> exporter) {

            }

            @Override
            public boolean removeExportMap(String key, Exporter<?> exporter) {
                return true;
            }

            @Override
            public Collection<Exporter<?>> getExporters() {
                return null;
            }
        });
        assertFalse(injvmInvoker.isAvailable());

    }

    @Test
    public void testLocalProtocolWithToken() throws Exception {
        DemoService service = new DemoServiceImpl();
        Invoker<?> invoker = proxy.getInvoker(service, DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService?token=abc").addParameter(INTERFACE_KEY, DemoService.class.getName()));
        assertTrue(invoker.isAvailable());
        Exporter<?> exporter = protocol.export(invoker);
        exporters.add(exporter);
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService").addParameter(INTERFACE_KEY, DemoService.class.getName())));
        assertEquals(service.getSize(new String[]{"", "", ""}), 3);
    }

    @Test
    public void testIsInjvmRefer() throws Exception {
        DemoService service = new DemoServiceImpl();
        URL url = URL.valueOf("injvm://127.0.0.1/TestService")
                .addParameter(INTERFACE_KEY, DemoService.class.getName());
        Exporter<?> exporter = protocol.export(proxy.getInvoker(service, DemoService.class, url));
        exporters.add(exporter);

        url = url.setProtocol("dubbo");
        assertTrue(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));

        url = url.addParameter(GROUP_KEY, "*")
                .addParameter(VERSION_KEY, "*");
        assertTrue(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));

        url = URL.valueOf("fake://127.0.0.1/TestService").addParameter(SCOPE_KEY, SCOPE_LOCAL);
        assertTrue(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));

        url = URL.valueOf("fake://127.0.0.1/TestService").addParameter(LOCAL_PROTOCOL, true);
        assertTrue(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));

        url = URL.valueOf("fake://127.0.0.1/TestService").addParameter(SCOPE_KEY, SCOPE_REMOTE);
        assertFalse(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));

        url = URL.valueOf("fake://127.0.0.1/TestService").addParameter(GENERIC_KEY, true);
        assertFalse(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));

        url = URL.valueOf("fake://127.0.0.1/TestService").addParameter("cluster", "broadcast");
        assertFalse(InjvmProtocol.getInjvmProtocol().isInjvmRefer(url));
    }


    @Test
    public void testRemoteApplicationName() throws Exception {
        DemoService service = new DemoServiceImpl();
        URL url = URL.valueOf("injvm://127.0.0.1/TestService").addParameter(INTERFACE_KEY, DemoService.class.getName()).addParameter("application", "consumer");
        Invoker<?> invoker = proxy.getInvoker(service, DemoService.class, url);
        assertTrue(invoker.isAvailable());
        Exporter<?> exporter = protocol.export(invoker);
        exporters.add(exporter);
        service = proxy.getProxy(protocol.refer(DemoService.class, url));
        assertEquals(service.getRemoteApplicationName(), "consumer");
    }

    @Test
    public void testLocalProtocolAsync() throws Exception {
        DemoService service = new DemoServiceImpl();
        URL url = URL.valueOf("injvm://127.0.0.1/TestService")
                .addParameter(ASYNC_KEY, true)
                .addParameter(INTERFACE_KEY, DemoService.class.getName()).addParameter("application", "consumer");
        Invoker<?> invoker = proxy.getInvoker(service, DemoService.class, url);
        assertTrue(invoker.isAvailable());
        Exporter<?> exporter = protocol.export(invoker);
        exporters.add(exporter);
        service = proxy.getProxy(protocol.refer(DemoService.class, url));
        assertNull(service.getAsyncResult());
    }
}
