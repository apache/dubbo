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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReferenceConfigTest {

    @BeforeEach
    public void setUp() {
        ConfigManager.getInstance().clear();
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

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            demoService.export();
            rc.get();
            Assertions.assertTrue(!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(
                    rc.getInvoker().getUrl().getProtocol()));
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
}
