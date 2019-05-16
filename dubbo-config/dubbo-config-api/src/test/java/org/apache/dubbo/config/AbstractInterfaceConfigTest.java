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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.mock.GreetingLocal1;
import org.apache.dubbo.config.mock.GreetingLocal2;
import org.apache.dubbo.config.mock.GreetingLocal3;
import org.apache.dubbo.config.mock.GreetingMock1;
import org.apache.dubbo.config.mock.GreetingMock2;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.registry.RegistryService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.apache.dubbo.common.constants.ConfigConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.ConfigConstants.SHUTDOWN_WAIT_SECONDS_KEY;

public class AbstractInterfaceConfigTest {
    private static File dubboProperties;

    @BeforeAll
    public static void setUp(@TempDir Path folder) {
        dubboProperties = folder.resolve(CommonConstants.DUBBO_PROPERTIES_KEY).toFile();
        System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, dubboProperties.getAbsolutePath());
    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
    }

    @AfterEach
    public void tearMethodAfterEachUT() {
        ConfigManager.getInstance().clear();
    }

    @Test
    public void testCheckRegistry1() {
        System.setProperty("dubbo.registry.address", "addr1|addr2");
        try {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkRegistry();
            Assertions.assertEquals(2, interfaceConfig.getRegistries().size());
        } finally {
            System.clearProperty("dubbo.registry.address");
        }
    }

    @Test
    public void testCheckRegistry2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkRegistry();
        });
    }

    @Test
    public void checkApplication1() {
        try {
            ConfigUtils.setProperties(null);
            System.clearProperty(SHUTDOWN_WAIT_KEY);
            System.clearProperty(SHUTDOWN_WAIT_SECONDS_KEY);

            writeDubboProperties(SHUTDOWN_WAIT_KEY, "100");
            System.setProperty("dubbo.application.name", "demo");
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkApplication();
            ApplicationConfig appConfig = interfaceConfig.getApplication();
            Assertions.assertEquals("demo", appConfig.getName());
            Assertions.assertEquals("100", System.getProperty(SHUTDOWN_WAIT_KEY));

            System.clearProperty(SHUTDOWN_WAIT_KEY);
            ConfigUtils.setProperties(null);
            writeDubboProperties(SHUTDOWN_WAIT_SECONDS_KEY, "1000");
            System.setProperty("dubbo.application.name", "demo");
            interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkApplication();
            Assertions.assertEquals("1000", System.getProperty(SHUTDOWN_WAIT_SECONDS_KEY));
        } finally {
            ConfigUtils.setProperties(null);
            System.clearProperty("dubbo.application.name");
            System.clearProperty(SHUTDOWN_WAIT_KEY);
            System.clearProperty(SHUTDOWN_WAIT_SECONDS_KEY);
        }
    }

    @Test
    public void checkApplication2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkApplication();
        });
    }

    @Test
    public void testLoadRegistries() {
        System.setProperty("dubbo.registry.address", "addr1");
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        // FIXME: now we need to check first, then load
        interfaceConfig.checkRegistry();
        List<URL> urls = interfaceConfig.loadRegistries(true);
        Assertions.assertEquals(1, urls.size());
        URL url = urls.get(0);
        Assertions.assertEquals("registry", url.getProtocol());
        Assertions.assertEquals("addr1:9090", url.getAddress());
        Assertions.assertEquals(RegistryService.class.getName(), url.getPath());
        Assertions.assertTrue(url.getParameters().containsKey("timestamp"));
        Assertions.assertTrue(url.getParameters().containsKey("pid"));
        Assertions.assertTrue(url.getParameters().containsKey("registry"));
        Assertions.assertTrue(url.getParameters().containsKey("dubbo"));
    }

    @Test
    public void testLoadMonitor() {
        System.setProperty("dubbo.monitor.address", "monitor-addr:12080");
        System.setProperty("dubbo.monitor.protocol", "monitor");
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        URL url = interfaceConfig.loadMonitor(new URL("dubbo", "addr1", 9090));
        Assertions.assertEquals("monitor-addr:12080", url.getAddress());
        Assertions.assertEquals(MonitorService.class.getName(), url.getParameter("interface"));
        Assertions.assertNotNull(url.getParameter("dubbo"));
        Assertions.assertNotNull(url.getParameter("pid"));
        Assertions.assertNotNull(url.getParameter("timestamp"));
    }

    @Test
    public void checkInterfaceAndMethods1() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkInterfaceAndMethods(null, null);
        });
    }

    @Test
    public void checkInterfaceAndMethods2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkInterfaceAndMethods(AbstractInterfaceConfigTest.class, null);
        });
    }

    @Test
    public void checkInterfaceAndMethod3() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            MethodConfig methodConfig = new MethodConfig();
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkInterfaceAndMethods(Greeting.class, Collections.singletonList(methodConfig));
        });
    }

    @Test
    public void checkInterfaceAndMethod4() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setName("nihao");
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.checkInterfaceAndMethods(Greeting.class, Collections.singletonList(methodConfig));
        });
    }

    @Test
    public void checkInterfaceAndMethod5() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("hello");
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.checkInterfaceAndMethods(Greeting.class, Collections.singletonList(methodConfig));
    }

    @Test
    public void checkStubAndMock1() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setLocal(GreetingLocal1.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
            interfaceConfig.checkMock(Greeting.class);
        });
    }

    @Test
    public void checkStubAndMock2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setLocal(GreetingLocal2.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
            interfaceConfig.checkMock(Greeting.class);
        });
    }

    @Test
    public void checkStubAndMock3() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setLocal(GreetingLocal3.class.getName());
        interfaceConfig.checkStubAndLocal(Greeting.class);
        interfaceConfig.checkMock(Greeting.class);
    }

    @Test
    public void checkStubAndMock4() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setStub(GreetingLocal1.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
            interfaceConfig.checkMock(Greeting.class);
        });
    }

    @Test
    public void checkStubAndMock5() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setStub(GreetingLocal2.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
            interfaceConfig.checkMock(Greeting.class);
        });
    }

    @Test
    public void checkStubAndMock6() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setStub(GreetingLocal3.class.getName());
        interfaceConfig.checkStubAndLocal(Greeting.class);
        interfaceConfig.checkMock(Greeting.class);
    }

    @Test
    public void checkStubAndMock7() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setMock("return {a, b}");
            interfaceConfig.checkStubAndLocal(Greeting.class);
            interfaceConfig.checkMock(Greeting.class);
        });
    }

    @Test
    public void checkStubAndMock8() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setMock(GreetingMock1.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
            interfaceConfig.checkMock(Greeting.class);
        });
    }

    @Test
    public void checkStubAndMock9() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setMock(GreetingMock2.class.getName());
            interfaceConfig.checkStubAndLocal(Greeting.class);
            interfaceConfig.checkMock(Greeting.class);
        });
    }

    @Test
    public void testLocal() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setLocal((Boolean) null);
        Assertions.assertNull(interfaceConfig.getLocal());
        interfaceConfig.setLocal(true);
        Assertions.assertEquals("true", interfaceConfig.getLocal());
        interfaceConfig.setLocal("GreetingMock");
        Assertions.assertEquals("GreetingMock", interfaceConfig.getLocal());
    }

    @Test
    public void testStub() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setStub((Boolean) null);
        Assertions.assertNull(interfaceConfig.getStub());
        interfaceConfig.setStub(true);
        Assertions.assertEquals("true", interfaceConfig.getStub());
        interfaceConfig.setStub("GreetingMock");
        Assertions.assertEquals("GreetingMock", interfaceConfig.getStub());
    }

    @Test
    public void testCluster() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setCluster("mockcluster");
        Assertions.assertEquals("mockcluster", interfaceConfig.getCluster());
    }

    @Test
    public void testProxy() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setProxy("mockproxyfactory");
        Assertions.assertEquals("mockproxyfactory", interfaceConfig.getProxy());
    }

    @Test
    public void testConnections() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setConnections(1);
        Assertions.assertEquals(1, interfaceConfig.getConnections().intValue());
    }

    @Test
    public void testFilter() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setFilter("mockfilter");
        Assertions.assertEquals("mockfilter", interfaceConfig.getFilter());
    }

    @Test
    public void testListener() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setListener("mockinvokerlistener");
        Assertions.assertEquals("mockinvokerlistener", interfaceConfig.getListener());
    }

    @Test
    public void testLayer() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setLayer("layer");
        Assertions.assertEquals("layer", interfaceConfig.getLayer());
    }

    @Test
    public void testApplication() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        ApplicationConfig applicationConfig = new ApplicationConfig();
        interfaceConfig.setApplication(applicationConfig);
        Assertions.assertSame(applicationConfig, interfaceConfig.getApplication());
    }

    @Test
    public void testModule() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        ModuleConfig moduleConfig = new ModuleConfig();
        interfaceConfig.setModule(moduleConfig);
        Assertions.assertSame(moduleConfig, interfaceConfig.getModule());
    }

    @Test
    public void testRegistry() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        RegistryConfig registryConfig = new RegistryConfig();
        interfaceConfig.setRegistry(registryConfig);
        Assertions.assertSame(registryConfig, interfaceConfig.getRegistry());
    }

    @Test
    public void testRegistries() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        RegistryConfig registryConfig = new RegistryConfig();
        interfaceConfig.setRegistries(Collections.singletonList(registryConfig));
        Assertions.assertEquals(1, interfaceConfig.getRegistries().size());
        Assertions.assertSame(registryConfig, interfaceConfig.getRegistries().get(0));
    }

    @Test
    public void testMonitor() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setMonitor("monitor-addr");
        Assertions.assertEquals("monitor-addr", interfaceConfig.getMonitor().getAddress());
        MonitorConfig monitorConfig = new MonitorConfig();
        interfaceConfig.setMonitor(monitorConfig);
        Assertions.assertSame(monitorConfig, interfaceConfig.getMonitor());
    }

    @Test
    public void testOwner() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setOwner("owner");
        Assertions.assertEquals("owner", interfaceConfig.getOwner());
    }

    @Test
    public void testCallbacks() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setCallbacks(2);
        Assertions.assertEquals(2, interfaceConfig.getCallbacks().intValue());
    }

    @Test
    public void testOnconnect() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setOnconnect("onConnect");
        Assertions.assertEquals("onConnect", interfaceConfig.getOnconnect());
    }

    @Test
    public void testOndisconnect() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setOndisconnect("onDisconnect");
        Assertions.assertEquals("onDisconnect", interfaceConfig.getOndisconnect());
    }

    @Test
    public void testScope() {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setScope("scope");
        Assertions.assertEquals("scope", interfaceConfig.getScope());
    }

    private void writeDubboProperties(String key, String value) {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(dubboProperties));
            Properties properties = new Properties();
            properties.put(key, value);
            properties.store(os, "");
            os.close();
        } catch (IOException e) {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    private static class InterfaceConfig extends AbstractInterfaceConfig {

    }
}
