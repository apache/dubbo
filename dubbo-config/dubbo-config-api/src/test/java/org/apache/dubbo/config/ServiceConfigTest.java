/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.mock.MockProtocol2;
import org.apache.dubbo.config.mock.MockRegistryFactory2;
import org.apache.dubbo.config.mock.MockServiceListener;
import org.apache.dubbo.config.mock.TestProxyFactory;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.service.GenericService;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_BEAN;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_DEFAULT;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_NATIVE_JAVA;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.config.Constants.SHUTDOWN_TIMEOUT_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.withSettings;

public class ServiceConfigTest {
    private Protocol protocolDelegate = Mockito.mock(Protocol.class);
    private Registry registryDelegate = Mockito.mock(Registry.class);
    private Exporter exporter = Mockito.mock(Exporter.class);
    private ServiceConfig<DemoServiceImpl> service;
    private ServiceConfig<DemoServiceImpl> service2;
    private ServiceConfig<DemoServiceImpl> serviceWithoutRegistryConfig;
    private ServiceConfig<DemoServiceImpl> delayService;

    @BeforeEach
    public void setUp() throws Exception {
        DubboBootstrap.reset();

        service = new ServiceConfig<>();
        service2 = new ServiceConfig<>();
        serviceWithoutRegistryConfig = new ServiceConfig<>();
        delayService = new ServiceConfig<>();

        MockProtocol2.delegate = protocolDelegate;
        MockRegistryFactory2.registry = registryDelegate;
        Mockito.when(protocolDelegate.export(Mockito.any(Invoker.class))).thenReturn(exporter);

        ApplicationConfig app = new ApplicationConfig("app");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("mockprotocol2");

        ProviderConfig provider = new ProviderConfig();
        provider.setExport(true);
        provider.setProtocol(protocolConfig);

        RegistryConfig registry = new RegistryConfig();
        registry.setProtocol("mockprotocol2");
        registry.setAddress("N/A");

        ArgumentConfig argument = new ArgumentConfig();
        argument.setIndex(0);
        argument.setCallback(false);

        MethodConfig method = new MethodConfig();
        method.setName("echo");
        method.setArguments(Collections.singletonList(argument));

        service.setProvider(provider);
        service.setApplication(app);
        service.setRegistry(registry);
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setMethods(Collections.singletonList(method));
        service.setGroup("demo1");

        service2.setProvider(provider);
        service2.setApplication(app);
        service2.setRegistry(registry);
        service2.setInterface(DemoService.class);
        service2.setRef(new DemoServiceImpl());
        service2.setMethods(Collections.singletonList(method));
        service2.setProxy("testproxyfactory");
        service2.setGroup("demo2");

        delayService.setProvider(provider);
        delayService.setApplication(app);
        delayService.setRegistry(registry);
        delayService.setInterface(DemoService.class);
        delayService.setRef(new DemoServiceImpl());
        delayService.setMethods(Collections.singletonList(method));
        delayService.setDelay(100);
        delayService.setGroup("demo3");

        serviceWithoutRegistryConfig.setProvider(provider);
        serviceWithoutRegistryConfig.setApplication(app);
        serviceWithoutRegistryConfig.setInterface(DemoService.class);
        serviceWithoutRegistryConfig.setRef(new DemoServiceImpl());
        serviceWithoutRegistryConfig.setMethods(Collections.singletonList(method));
        serviceWithoutRegistryConfig.setGroup("demo4");
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testExport() throws Exception {
        service.export();

        assertThat(service.getExportedUrls(), hasSize(1));
        URL url = service.toUrl();
        assertThat(url.getProtocol(), equalTo("mockprotocol2"));
        assertThat(url.getPath(), equalTo(DemoService.class.getName()));
        assertThat(url.getParameters(), hasEntry(ANYHOST_KEY, "true"));
        assertThat(url.getParameters(), hasEntry(APPLICATION_KEY, "app"));
        assertThat(url.getParameters(), hasKey(BIND_IP_KEY));
        assertThat(url.getParameters(), hasKey(BIND_PORT_KEY));
        assertThat(url.getParameters(), hasEntry(EXPORT_KEY, "true"));
        assertThat(url.getParameters(), hasEntry("echo.0.callback", "false"));
        assertThat(url.getParameters(), hasEntry(GENERIC_KEY, "false"));
        assertThat(url.getParameters(), hasEntry(INTERFACE_KEY, DemoService.class.getName()));
        assertThat(url.getParameters(), hasKey(METHODS_KEY));
        assertThat(url.getParameters().get(METHODS_KEY), containsString("echo"));
        assertThat(url.getParameters(), hasEntry(SIDE_KEY, PROVIDER));
        Mockito.verify(protocolDelegate).export(Mockito.any(Invoker.class));
    }

    @Test
    public void testVersionAndGroupConfigFromProvider() {
        //Service no configuration version , the Provider configured.
        service.getProvider().setVersion("1.0.0");
        service.getProvider().setGroup("groupA");
        service.export();

        String serviceVersion = service.getVersion();
        String serviceVersion2 = service.toUrl().getVersion();

        String group = service.getGroup();
        String group2 = service.toUrl().getGroup();

        assertEquals(serviceVersion2, serviceVersion);
        assertEquals(group, group2);
    }

    @Test
    public void testProxy() throws Exception {
        service2.export();

        assertThat(service2.getExportedUrls(), hasSize(1));
        assertEquals(2, TestProxyFactory.count); // local injvm and registry protocol, so expected is 2
        TestProxyFactory.count = 0;
    }


    @Test
    public void testDelayExport() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        delayService.addServiceListener(new ServiceListener() {
            @Override
            public void exported(ServiceConfig sc) {
                assertEquals(delayService, sc);
                assertThat(delayService.getExportedUrls(), hasSize(1));
                latch.countDown();
            }

            @Override
            public void unexported(ServiceConfig sc) {

            }
        });
        delayService.export();
        assertTrue(delayService.getExportedUrls().isEmpty());
        latch.await();
    }

    @Test
    public void testUnexport() throws Exception {
        System.setProperty(SHUTDOWN_WAIT_KEY, "0");
        try {
            service.export();
            service.unexport();
            Thread.sleep(1000);
            Mockito.verify(exporter, Mockito.atLeastOnce()).unexport();
        } finally {
            System.clearProperty(SHUTDOWN_TIMEOUT_KEY);
        }
    }

    @Test
    public void testInterfaceClass() throws Exception {
        ServiceConfig<Greeting> service = new ServiceConfig<>();
        service.setInterface(Greeting.class.getName());
        service.setRef(Mockito.mock(Greeting.class));
        assertThat(service.getInterfaceClass() == Greeting.class, is(true));
        service = new ServiceConfig<>();
        service.setRef(Mockito.mock(Greeting.class, withSettings().extraInterfaces(GenericService.class)));
        assertThat(service.getInterfaceClass() == GenericService.class, is(true));
    }

    @Test
    public void testInterface1() throws Exception {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ServiceConfig<DemoService> service = new ServiceConfig<>();
            service.setInterface(DemoServiceImpl.class);
        });
    }

    @Test
    public void testInterface2() throws Exception {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        assertThat(service.getInterface(), equalTo(DemoService.class.getName()));
    }

    @Test
    public void testProvider() throws Exception {
        ServiceConfig service = new ServiceConfig();
        ProviderConfig provider = new ProviderConfig();
        service.setProvider(provider);
        assertThat(service.getProvider(), is(provider));
    }

    @Test
    public void testGeneric1() throws Exception {
        ServiceConfig service = new ServiceConfig();
        service.setGeneric(GENERIC_SERIALIZATION_DEFAULT);
        assertThat(service.getGeneric(), equalTo(GENERIC_SERIALIZATION_DEFAULT));
        service.setGeneric(GENERIC_SERIALIZATION_NATIVE_JAVA);
        assertThat(service.getGeneric(), equalTo(GENERIC_SERIALIZATION_NATIVE_JAVA));
        service.setGeneric(GENERIC_SERIALIZATION_BEAN);
        assertThat(service.getGeneric(), equalTo(GENERIC_SERIALIZATION_BEAN));
    }

    @Test
    public void testGeneric2() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ServiceConfig service = new ServiceConfig();
            service.setGeneric("illegal");
        });
    }

//    @Test
//    public void testMock() throws Exception {
//        Assertions.assertThrows(IllegalArgumentException.class, () -> {
//            ServiceConfig service = new ServiceConfig();
//            service.setMock("true");
//        });
//    }
//
//    @Test
//    public void testMock2() throws Exception {
//        Assertions.assertThrows(IllegalArgumentException.class, () -> {
//            ServiceConfig service = new ServiceConfig();
//            service.setMock(true);
//        });
//    }

    @Test
    public void testApplicationInUrl() {
        service.export();
        assertNotNull(service.toUrl().getApplication());
        Assertions.assertEquals("app", service.toUrl().getApplication());
    }

    @Test
    public void testMetaData() {
        // test new instance
        ServiceConfig config = new ServiceConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: " + metaData);

        // test merged and override provider attributes
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setAsync(true);
        providerConfig.setActives(10);
        config.setProvider(providerConfig);
        config.setAsync(false);// override

        metaData = config.getMetaData();
        Assertions.assertEquals(2, metaData.size());
        Assertions.assertEquals("" + providerConfig.getActives(), metaData.get("actives"));
        Assertions.assertEquals("" + config.isAsync(), metaData.get("async"));
    }


    @Test
    public void testExportWithoutRegistryConfig() {
        serviceWithoutRegistryConfig.export();

        assertThat(serviceWithoutRegistryConfig.getExportedUrls(), hasSize(1));
        URL url = serviceWithoutRegistryConfig.toUrl();
        assertThat(url.getProtocol(), equalTo("mockprotocol2"));
        assertThat(url.getPath(), equalTo(DemoService.class.getName()));
        assertThat(url.getParameters(), hasEntry(ANYHOST_KEY, "true"));
        assertThat(url.getParameters(), hasEntry(APPLICATION_KEY, "app"));
        assertThat(url.getParameters(), hasKey(BIND_IP_KEY));
        assertThat(url.getParameters(), hasKey(BIND_PORT_KEY));
        assertThat(url.getParameters(), hasEntry(EXPORT_KEY, "true"));
        assertThat(url.getParameters(), hasEntry("echo.0.callback", "false"));
        assertThat(url.getParameters(), hasEntry(GENERIC_KEY, "false"));
        assertThat(url.getParameters(), hasEntry(INTERFACE_KEY, DemoService.class.getName()));
        assertThat(url.getParameters(), hasKey(METHODS_KEY));
        assertThat(url.getParameters().get(METHODS_KEY), containsString("echo"));
        assertThat(url.getParameters(), hasEntry(SIDE_KEY, PROVIDER));
        Mockito.verify(protocolDelegate).export(Mockito.any(Invoker.class));
    }

    @Test
    public void testServiceListener() {
        ExtensionLoader<ServiceListener> extensionLoader = ExtensionLoader.getExtensionLoader(ServiceListener.class);
        MockServiceListener mockServiceListener = (MockServiceListener) extensionLoader.getExtension("mock");
        assertNotNull(mockServiceListener);
        mockServiceListener.clearExportedServices();

        service.export();

        Map<String, ServiceConfig> exportedServices = mockServiceListener.getExportedServices();
        assertEquals(1, exportedServices.size());
        ServiceConfig serviceConfig = exportedServices.get(service.getUniqueServiceName());
        assertSame(service, serviceConfig);
    }


    @Test
    public void testMethodConfigWithInvalidArgumentConfig() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setProtocol(new ProtocolConfig() {{
                setName("dubbo");
            }});

            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setName("sayName");
            // invalid argument index.
            methodConfig.setArguments(Lists.newArrayList(new ArgumentConfig() {{
                // unset config.
            }}));
            service.setMethods(Lists.newArrayList(methodConfig));

            service.export();
        });
    }

    @Test
    public void testMethodConfigWithConfiguredArgumentTypeAndIndex() {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setProtocol(new ProtocolConfig() {{
            setName("dubbo");
        }});

        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayName");
        // invalid argument index.
        methodConfig.setArguments(Lists.newArrayList(new ArgumentConfig() {{
            setType(String.class.getName());
            setIndex(0);
            setCallback(false);
        }}));
        service.setMethods(Lists.newArrayList(methodConfig));

        service.export();

        assertFalse(service.getExportedUrls().isEmpty());
        assertEquals("false", service.getExportedUrls().get(0).getParameters().get("sayName.0.callback"));
    }

    @Test
    public void testMethodConfigWithConfiguredArgumentIndex() {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setProtocol(new ProtocolConfig() {{
            setName("dubbo");
        }});

        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayName");
        // invalid argument index.
        methodConfig.setArguments(Lists.newArrayList(new ArgumentConfig() {{
            setIndex(0);
            setCallback(false);
        }}));
        service.setMethods(Lists.newArrayList(methodConfig));

        service.export();

        assertFalse(service.getExportedUrls().isEmpty());
        assertEquals("false", service.getExportedUrls().get(0).getParameters().get("sayName.0.callback"));
    }

    @Test
    public void testMethodConfigWithConfiguredArgumentType() {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setProtocol(new ProtocolConfig() {{
            setName("dubbo");
        }});

        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayName");
        // invalid argument index.
        methodConfig.setArguments(Lists.newArrayList(new ArgumentConfig() {{
            setType(String.class.getName());
            setCallback(false);
        }}));
        service.setMethods(Lists.newArrayList(methodConfig));

        service.export();

        assertFalse(service.getExportedUrls().isEmpty());
        assertEquals("false", service.getExportedUrls().get(0).getParameters().get("sayName.0.callback"));
    }

    @Test
    public void testMethodConfigWithUnknownArgumentType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setProtocol(new ProtocolConfig() {{
                setName("dubbo");
            }});

            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setName("sayName");
            // invalid argument index.
            methodConfig.setArguments(Lists.newArrayList(new ArgumentConfig() {{
                setType(Integer.class.getName());
                setCallback(false);
            }}));
            service.setMethods(Lists.newArrayList(methodConfig));

            service.export();
        });
    }

    @Test
    public void testMethodConfigWithUnmatchedArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setProtocol(new ProtocolConfig() {{
                setName("dubbo");
            }});

            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setName("sayName");
            // invalid argument index.
            methodConfig.setArguments(Lists.newArrayList(new ArgumentConfig() {{
                setType(Integer.class.getName());
                setIndex(0);
            }}));
            service.setMethods(Lists.newArrayList(methodConfig));

            service.export();
        });
    }

    @Test
    public void testMethodConfigWithInvalidArgumentIndex() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setProtocol(new ProtocolConfig() {{
                setName("dubbo");
            }});

            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setName("sayName");
            // invalid argument index.
            methodConfig.setArguments(Lists.newArrayList(new ArgumentConfig() {{
                setType(String.class.getName());
                setIndex(1);
            }}));
            service.setMethods(Lists.newArrayList(methodConfig));

            service.export();
        });
    }
}
