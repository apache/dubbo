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
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.mock.GreetingLocal1;
import org.apache.dubbo.config.mock.GreetingLocal2;
import org.apache.dubbo.config.mock.GreetingLocal3;
import org.apache.dubbo.config.mock.GreetingMock1;
import org.apache.dubbo.config.mock.GreetingMock2;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

public class AbstractInterfaceConfigTest {
    private static File dubboProperties;

    @BeforeAll
    public static void setUp(@TempDir Path folder) {
        ApplicationModel.reset();
        dubboProperties = folder.resolve(CommonConstants.DUBBO_PROPERTIES_KEY).toFile();
        System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, dubboProperties.getAbsolutePath());
    }

    @AfterAll
    public static void tearDown() {
        ApplicationModel.reset();
        System.clearProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
    }

    @AfterEach
    public void tearMethodAfterEachUT() {
//        ApplicationModel.getConfigManager().clear();
    }

    @Test
    public void testCheckRegistry1() {
        System.setProperty("dubbo.registry.address", "addr1");
        try {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setApplication(new ApplicationConfig("testCheckRegistry1"));
            config.checkRegistry();
            Assertions.assertEquals(1, config.getRegistries().size());
            Assertions.assertEquals("addr1", config.getRegistries().get(0).getAddress());
        } finally {
            System.clearProperty("dubbo.registry.address");
        }
    }

    @Test
    public void testCheckRegistry2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.checkRegistry();
        });
    }

    @Test
    public void checkInterfaceAndMethods1() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.checkInterfaceAndMethods(null, null);
        });
    }

    @Test
    public void checkInterfaceAndMethods2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.checkInterfaceAndMethods(AbstractInterfaceConfigTest.class, null);
        });
    }

    @Test
    public void checkInterfaceAndMethod3() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            MethodConfig methodConfig = new MethodConfig();
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.checkInterfaceAndMethods(Greeting.class, Collections.singletonList(methodConfig));
        });
    }

    @Test
    public void checkInterfaceAndMethod4() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setName("nihao");
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.checkInterfaceAndMethods(Greeting.class, Collections.singletonList(methodConfig));
        });
    }

    @Test
    public void checkInterfaceAndMethod5() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("hello");
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.checkInterfaceAndMethods(Greeting.class, Collections.singletonList(methodConfig));
    }

    @Test
    public void checkStubAndMock1() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setLocal(GreetingLocal1.class.getName());
            config.checkStubAndLocal(Greeting.class);
            ConfigValidationUtils.checkMock(Greeting.class, config);
        });
    }

    @Test
    public void checkStubAndMock2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setLocal(GreetingLocal2.class.getName());
            config.checkStubAndLocal(Greeting.class);
            ConfigValidationUtils.checkMock(Greeting.class, config);
        });
    }

    @Test
    public void checkStubAndMock3() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setLocal(GreetingLocal3.class.getName());
        config.checkStubAndLocal(Greeting.class);
        ConfigValidationUtils.checkMock(Greeting.class, config);
    }

    @Test
    public void checkStubAndMock4() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setStub(GreetingLocal1.class.getName());
            config.checkStubAndLocal(Greeting.class);
            ConfigValidationUtils.checkMock(Greeting.class, config);
        });
    }

    @Test
    public void checkStubAndMock5() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setStub(GreetingLocal2.class.getName());
            config.checkStubAndLocal(Greeting.class);
            ConfigValidationUtils.checkMock(Greeting.class, config);
        });
    }

    @Test
    public void checkStubAndMock6() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setStub(GreetingLocal3.class.getName());
        config.checkStubAndLocal(Greeting.class);
        ConfigValidationUtils.checkMock(Greeting.class, config);
    }

    @Test
    public void checkStubAndMock7() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setMock("return {a, b}");
            config.checkStubAndLocal(Greeting.class);
            ConfigValidationUtils.checkMock(Greeting.class, config);
        });
    }

    @Test
    public void checkStubAndMock8() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setMock(GreetingMock1.class.getName());
            config.checkStubAndLocal(Greeting.class);
            ConfigValidationUtils.checkMock(Greeting.class, config);
        });
    }

    @Test
    public void checkStubAndMock9() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
            config.setMock(GreetingMock2.class.getName());
            config.checkStubAndLocal(Greeting.class);
            ConfigValidationUtils.checkMock(Greeting.class, config);
        });
    }

    @Test
    public void testLocal() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setLocal((Boolean) null);
        Assertions.assertNull(config.getLocal());
        config.setLocal(true);
        Assertions.assertEquals("true", config.getLocal());
        config.setLocal("GreetingMock");
        Assertions.assertEquals("GreetingMock", config.getLocal());
    }

    @Test
    public void testStub() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setStub((Boolean) null);
        Assertions.assertNull(config.getStub());
        config.setStub(true);
        Assertions.assertEquals("true", config.getStub());
        config.setStub("GreetingMock");
        Assertions.assertEquals("GreetingMock", config.getStub());
    }

    @Test
    public void testCluster() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setCluster("mockcluster");
        Assertions.assertEquals("mockcluster", config.getCluster());
    }

    @Test
    public void testProxy() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setProxy("mockproxyfactory");
        Assertions.assertEquals("mockproxyfactory", config.getProxy());
    }

    @Test
    public void testConnections() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setConnections(1);
        Assertions.assertEquals(1, config.getConnections().intValue());
    }

    @Test
    public void testFilter() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setFilter("mockfilter");
        Assertions.assertEquals("mockfilter", config.getFilter());
    }

    @Test
    public void testListener() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setListener("mockinvokerlistener");
        Assertions.assertEquals("mockinvokerlistener", config.getListener());
    }

    @Test
    public void testLayer() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setLayer("layer");
        Assertions.assertEquals("layer", config.getLayer());
    }

    @Test
    public void testApplication() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        config.setApplication(applicationConfig);
        Assertions.assertSame(applicationConfig, config.getApplication());
    }

    @Test
    public void testModule() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        ModuleConfig moduleConfig = new ModuleConfig();
        config.setModule(moduleConfig);
        Assertions.assertSame(moduleConfig, config.getModule());
    }

    @Test
    public void testRegistry() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        RegistryConfig registryConfig = new RegistryConfig();
        config.setRegistry(registryConfig);
        Assertions.assertSame(registryConfig, config.getRegistry());
    }

    @Test
    public void testRegistries() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        RegistryConfig registryConfig = new RegistryConfig();
        config.setRegistries(Collections.singletonList(registryConfig));
        Assertions.assertEquals(1, config.getRegistries().size());
        Assertions.assertSame(registryConfig, config.getRegistries().get(0));
    }

    @Test
    public void testMonitor() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setMonitor("monitor-addr");
        Assertions.assertEquals("monitor-addr", config.getMonitor().getAddress());
        MonitorConfig monitorConfig = new MonitorConfig();
        config.setMonitor(monitorConfig);
        Assertions.assertSame(monitorConfig, config.getMonitor());
    }

    @Test
    public void testOwner() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setOwner("owner");
        Assertions.assertEquals("owner", config.getOwner());
    }

    @Test
    public void testCallbacks() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setCallbacks(2);
        Assertions.assertEquals(2, config.getCallbacks().intValue());
    }

    @Test
    public void testOnconnect() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setOnconnect("onConnect");
        Assertions.assertEquals("onConnect", config.getOnconnect());
    }

    @Test
    public void testOndisconnect() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setOndisconnect("onDisconnect");
        Assertions.assertEquals("onDisconnect", config.getOndisconnect());
    }

    @Test
    public void testScope() {
        AbstractInterfaceConfig config = Mockito.spy(AbstractInterfaceConfig.class);
        config.setScope("scope");
        Assertions.assertEquals("scope", config.getScope());
    }
    
}
