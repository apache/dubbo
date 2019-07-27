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
package org.apache.dubbo.config;

import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.event.ReferenceConfigDestroyedEvent;
import org.apache.dubbo.config.event.ReferenceConfigInitializedEvent;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReferenceConfigTest {

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    @BeforeEach
    public void setUp() {
        ConfigManager.getInstance().clear();
        eventDispatcher.removeAllEventListeners();
    }

    @AfterEach
    public void tearDown() {
        ConfigManager.getInstance().clear();
    }

    @Test
    public void testInjvm() throws Exception {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-protocol-random-port");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("multicast://224.5.6.7:1234");

        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("mockprotocol");

        ServiceConfig<DemoService> demoService;
        demoService = new ServiceConfig<DemoService>();
        demoService.setInterface(DemoService.class);
        demoService.setRef(new DemoServiceImpl());
        demoService.setApplication(application);
        demoService.setRegistry(registry);
        demoService.setProtocol(protocol);

        ReferenceConfig<DemoService> rc = new ReferenceConfig<DemoService>();
        rc.setApplication(application);
        rc.setRegistry(registry);
        rc.setInterface(DemoService.class.getName());
        rc.setInjvm(false);

        AtomicReference<ReferenceConfig> reference = new AtomicReference<>();

        eventDispatcher.addEventListener(new EventListener<ReferenceConfigInitializedEvent>() {
            @Override
            public void onEvent(ReferenceConfigInitializedEvent event) {
                reference.set(event.getReferenceConfig());
            }
        });

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            demoService.export();
            rc.get();

            assertEquals(rc, reference.get());

            reference.compareAndSet(rc, null);

            Assertions.assertTrue(!LOCAL_PROTOCOL.equalsIgnoreCase(
                    rc.getInvoker().getUrl().getProtocol()));

            eventDispatcher.addEventListener(new EventListener<ReferenceConfigDestroyedEvent>() {
                @Override
                public void onEvent(ReferenceConfigDestroyedEvent event) {
                    reference.set(event.getReferenceConfig());
                }
            });

            rc.destroy();

            assertEquals(rc, reference.get());

        } finally {
            System.clearProperty("java.net.preferIPv4Stack");
            demoService.unexport();
        }
    }

    /**
     * unit test for dubbo-1765
     */
    @Test
    public void testReferenceRetry() {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-reference-retry");
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("multicast://224.5.6.7:1234");
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("mockprotocol");

        ReferenceConfig<DemoService> rc = new ReferenceConfig<DemoService>();
        rc.setApplication(application);
        rc.setRegistry(registry);
        rc.setInterface(DemoService.class.getName());

        boolean success = false;
        DemoService demoService = null;
        try {
            demoService = rc.get();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertFalse(success);
        Assertions.assertNull(demoService);

        ServiceConfig<DemoService> sc = new ServiceConfig<DemoService>();
        sc.setInterface(DemoService.class);
        sc.setRef(new DemoServiceImpl());
        sc.setApplication(application);
        sc.setRegistry(registry);
        sc.setProtocol(protocol);

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            sc.export();
            demoService = rc.get();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.clearProperty("java.net.preferIPv4Stack");
        }
        Assertions.assertTrue(success);
        Assertions.assertNotNull(demoService);

    }

    @Test
    public void testConstructWithReferenceAnnotation() throws NoSuchFieldException {
        Reference reference = getClass().getDeclaredField("innerTest").getAnnotation(Reference.class);
        ReferenceConfig referenceConfig = new ReferenceConfig(reference);
        Assertions.assertEquals(1, referenceConfig.getMethods().size());
        Assertions.assertEquals(((MethodConfig) referenceConfig.getMethods().get(0)).getName(), "sayHello");
        Assertions.assertEquals(1300, (int)((MethodConfig)referenceConfig.getMethods().get(0)).getTimeout());
        Assertions.assertEquals(4, (int)((MethodConfig)referenceConfig.getMethods().get(0)).getRetries());
        Assertions.assertEquals(((MethodConfig) referenceConfig.getMethods().get(0)).getLoadbalance(), "random");
        Assertions.assertEquals(3, (int)((MethodConfig)referenceConfig.getMethods().get(0)).getActives());
        Assertions.assertEquals(5, (int)((MethodConfig)referenceConfig.getMethods().get(0)).getExecutes());
        Assertions.assertTrue(((MethodConfig) referenceConfig.getMethods().get(0)).isAsync());
        assertEquals(((MethodConfig) referenceConfig.getMethods().get(0)).getOninvoke(), "i");
        assertEquals(((MethodConfig) referenceConfig.getMethods().get(0)).getOnreturn(), "r");
        assertEquals(((MethodConfig) referenceConfig.getMethods().get(0)).getOnthrow(), "t");
        assertEquals(((MethodConfig) referenceConfig.getMethods().get(0)).getCache(), "c");
    }


    @Reference(methods = {@Method(name = "sayHello", timeout = 1300, retries = 4, loadbalance = "random", async = true,
            actives = 3, executes = 5, deprecated = true, sticky = true, oninvoke = "i", onthrow = "t", onreturn = "r", cache = "c", validation = "v",
            arguments = {@Argument(index = 24, callback = true, type = "sss")})})
    private InnerTest innerTest;

    private class InnerTest {

    }
}
