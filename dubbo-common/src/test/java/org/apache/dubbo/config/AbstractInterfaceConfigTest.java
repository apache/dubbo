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


import org.apache.dubbo.common.constants.CommonConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

class AbstractInterfaceConfigTest {

    @BeforeAll
    public static void setUp(@TempDir Path folder) {
        File dubboProperties = folder.resolve(CommonConstants.DUBBO_PROPERTIES_KEY).toFile();
        System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, dubboProperties.getAbsolutePath());
    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
    }


    @Test
    void checkStub1() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setLocal(GreetingLocal1.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
        });
    }

    @Test
    void checkStub2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setLocal(GreetingLocal2.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
        });
    }

    @Test
    void checkStub3() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setLocal(GreetingLocal3.class.getName());
        interfaceConfig.checkStubAndLocal(Greeting.class);
    }

    @Test
    void checkStub4() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setStub(GreetingLocal1.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
        });
    }

    @Test
    void checkStub5() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setStub(GreetingLocal2.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
        });
    }

    @Test
    void checkStub6() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setStub(GreetingLocal3.class.getName());
        interfaceConfig.checkStubAndLocal(Greeting.class);
    }

    @Test
    void testLocal() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setLocal((Boolean) null);
        Assertions.assertNull(interfaceConfig.getLocal());
        interfaceConfig.setLocal(true);
        Assertions.assertEquals("true", interfaceConfig.getLocal());
        interfaceConfig.setLocal("GreetingMock");
        Assertions.assertEquals("GreetingMock", interfaceConfig.getLocal());
    }

    @Test
    void testStub() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setStub((Boolean) null);
        Assertions.assertNull(interfaceConfig.getStub());
        interfaceConfig.setStub(true);
        Assertions.assertEquals("true", interfaceConfig.getStub());
        interfaceConfig.setStub("GreetingMock");
        Assertions.assertEquals("GreetingMock", interfaceConfig.getStub());
    }

    @Test
    void testCluster() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setCluster("mockcluster");
        Assertions.assertEquals("mockcluster", interfaceConfig.getCluster());
    }

    @Test
    void testProxy() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setProxy("mockproxyfactory");
        Assertions.assertEquals("mockproxyfactory", interfaceConfig.getProxy());
    }

    @Test
    void testConnections() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setConnections(1);
        Assertions.assertEquals(1, interfaceConfig.getConnections().intValue());
    }

    @Test
    void testFilter() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setFilter("mockfilter");
        Assertions.assertEquals("mockfilter", interfaceConfig.getFilter());
    }

    @Test
    void testListener() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setListener("mockinvokerlistener");
        Assertions.assertEquals("mockinvokerlistener", interfaceConfig.getListener());
    }

    @Test
    void testLayer() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setLayer("layer");
        Assertions.assertEquals("layer", interfaceConfig.getLayer());
    }

    @Test
    void testApplication() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        ApplicationConfig applicationConfig = new ApplicationConfig("AbstractInterfaceConfigTest");
        interfaceConfig.setApplication(applicationConfig);
        Assertions.assertSame(applicationConfig, interfaceConfig.getApplication());
    }

    @Test
    void testModule() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        ModuleConfig moduleConfig = new ModuleConfig();
        interfaceConfig.setModule(moduleConfig);
        Assertions.assertSame(moduleConfig, interfaceConfig.getModule());
    }

    @Test
    void testRegistry() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        RegistryConfig registryConfig = new RegistryConfig();
        interfaceConfig.setRegistry(registryConfig);
        Assertions.assertSame(registryConfig, interfaceConfig.getRegistry());
    }

    @Test
    void testRegistries() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        RegistryConfig registryConfig = new RegistryConfig();
        interfaceConfig.setRegistries(Collections.singletonList(registryConfig));
        Assertions.assertEquals(1, interfaceConfig.getRegistries().size());
        Assertions.assertSame(registryConfig, interfaceConfig.getRegistries().get(0));
    }

    @Test
    void testMonitor() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setMonitor("monitor-addr");
        Assertions.assertEquals("monitor-addr", interfaceConfig.getMonitor().getAddress());
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setAddress("monitor-addr");
        interfaceConfig.setMonitor(monitorConfig);
        Assertions.assertSame(monitorConfig, interfaceConfig.getMonitor());
    }

    @Test
    void testOwner() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setOwner("owner");
        Assertions.assertEquals("owner", interfaceConfig.getOwner());
    }

    @Test
    void testCallbacks() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setCallbacks(2);
        Assertions.assertEquals(2, interfaceConfig.getCallbacks().intValue());
    }

    @Test
    void testOnconnect() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setOnconnect("onConnect");
        Assertions.assertEquals("onConnect", interfaceConfig.getOnconnect());
    }

    @Test
    void testOndisconnect() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setOndisconnect("onDisconnect");
        Assertions.assertEquals("onDisconnect", interfaceConfig.getOndisconnect());
    }

    @Test
    void testScope() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setScope("scope");
        Assertions.assertEquals("scope", interfaceConfig.getScope());
    }

    @Test
    void testVerifyMethod() {
        InterfaceConfig2 interfaceConfig2 = new InterfaceConfig2();
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setTimeout(5000);
        methodConfig.setName("sayHello");
        Class<?> clazz = Greeting.class;
        boolean verifyResult = interfaceConfig2.verifyMethodConfig(methodConfig, clazz, false);
        Assertions.assertTrue(verifyResult);

        boolean verifyResult2 = interfaceConfig2.verifyMethodConfig(methodConfig, clazz, true);
        Assertions.assertFalse(verifyResult2);
    }

    public static class InterfaceConfig2 extends AbstractInterfaceConfig {
        @Override
        protected boolean isNeedCheckMethod() {
            return false;
        }
    }

    public static class InterfaceConfig extends AbstractInterfaceConfig {

    }
}
