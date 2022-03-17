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
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.protocol.AsyncToSyncInvoker;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Check available status for dubboInvoker
 */
public class DubboInvokerAvailableTest {
    private static DubboProtocol protocol;
    private static ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {
        protocol = DubboProtocol.getDubboProtocol();
    }

    @AfterAll
    public static void tearDownAfterClass() {
        ProtocolUtils.closeAll();
    }

    @Test
    public void test_Normal_available() {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/org.apache.dubbo.rpc.protocol.dubbo.IDemoService");
        ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, url);

        DubboInvoker<?> invoker = (DubboInvoker<?>) protocol.protocolBindingRefer(IDemoService.class, url);
        Assertions.assertTrue(invoker.isAvailable());
        invoker.destroy();
        Assertions.assertFalse(invoker.isAvailable());
    }

    @Test
    public void test_Normal_ChannelReadOnly() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/org.apache.dubbo.rpc.protocol.dubbo.IDemoService");
        ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, url);

        DubboInvoker<?> invoker = (DubboInvoker<?>) protocol.protocolBindingRefer(IDemoService.class, url);
        Assertions.assertTrue(invoker.isAvailable());

        getClients(invoker)[0].setAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY, Boolean.TRUE);

        Assertions.assertFalse(invoker.isAvailable());

        // reset status since connection is shared among invokers
        getClients(invoker)[0].removeAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY);
    }

    @Disabled
    @Test
    public void test_normal_channel_close_wait_gracefully() throws Exception {
        int testPort = NetUtils.getAvailablePort();
        URL url = URL.valueOf("dubbo://127.0.0.1:" + testPort + "/org.apache.dubbo.rpc.protocol.dubbo.IDemoService?scope=true&lazy=false");
        Exporter<IDemoService> exporter = ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, url);
        Exporter<IDemoService> exporter0 = ProtocolUtils.export(new DemoServiceImpl0(), IDemoService.class, url);

        DubboInvoker<?> invoker = (DubboInvoker<?>) protocol.protocolBindingRefer(IDemoService.class, url);

        long start = System.currentTimeMillis();

        try {
            System.setProperty(SHUTDOWN_WAIT_KEY, "2000");
            protocol.destroy();
        } finally {
            System.getProperties().remove(SHUTDOWN_WAIT_KEY);
        }

        long waitTime = System.currentTimeMillis() - start;

        Assertions.assertTrue(waitTime >= 2000);
        Assertions.assertFalse(invoker.isAvailable());
    }

    @Test
    public void test_NoInvokers() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/org.apache.dubbo.rpc.protocol.dubbo.IDemoService?connections=1");
        ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, url);

        DubboInvoker<?> invoker = (DubboInvoker<?>) protocol.protocolBindingRefer(IDemoService.class, url);

        ExchangeClient[] clients = getClients(invoker);
        clients[0].close();
        Assertions.assertFalse(invoker.isAvailable());

    }

    @Test
    public void test_Lazy_ChannelReadOnly() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/org.apache.dubbo.rpc.protocol.dubbo.IDemoService?lazy=true&connections=1&timeout=10000");
        ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, url);

        AsyncToSyncInvoker<?> invoker = (AsyncToSyncInvoker) protocol.refer(IDemoService.class, url);
        Assertions.assertTrue(invoker.isAvailable());

        ExchangeClient exchangeClient = getClients((DubboInvoker<?>) invoker.getInvoker())[0];
        Assertions.assertFalse(exchangeClient.isClosed());
        try {
            exchangeClient.setAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY, Boolean.TRUE);
            fail();
        } catch (IllegalStateException e) {

        }
        //invoke method --> init client
        IDemoService service = (IDemoService) proxy.getProxy(invoker);
        Assertions.assertEquals("ok", service.get());

        Assertions.assertTrue(invoker.isAvailable());
        exchangeClient.setAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY, Boolean.TRUE);
        Assertions.assertFalse(invoker.isAvailable());
    }

    private ExchangeClient[] getClients(DubboInvoker<?> invoker) throws Exception {
        Field field = DubboInvoker.class.getDeclaredField("clients");
        ReflectUtils.makeAccessible(field);
        ExchangeClient[] clients = (ExchangeClient[]) field.get(invoker);
        Assertions.assertEquals(1, clients.length);
        return clients;
    }

    public class DemoServiceImpl implements IDemoService {
        public String get() {
            return "ok";
        }
    }

    public class DemoServiceImpl0 implements IDemoService {
        public String get() {
            return "ok";
        }
    }
}
