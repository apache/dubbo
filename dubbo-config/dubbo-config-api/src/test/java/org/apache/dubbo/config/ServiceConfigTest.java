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
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.mock.MockProtocol2;
import org.apache.dubbo.config.mock.MockRegistryFactory2;
import org.apache.dubbo.config.mock.TestProxyFactory;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.service.GenericService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.withSettings;

public class ServiceConfigTest {
    private Protocol protocolDelegate = Mockito.mock(Protocol.class);
    private Registry registryDelegate = Mockito.mock(Registry.class);
    private Exporter exporter = Mockito.mock(Exporter.class);
    private ServiceConfig<DemoServiceImpl> service = new ServiceConfig<DemoServiceImpl>();
    private ServiceConfig<DemoServiceImpl> service2 = new ServiceConfig<DemoServiceImpl>();
    private ServiceConfig<DemoServiceImpl> delayService = new ServiceConfig<DemoServiceImpl>();

    @BeforeEach
    public void setUp() throws Exception {
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

        service2.setProvider(provider);
        service2.setApplication(app);
        service2.setRegistry(registry);
        service2.setInterface(DemoService.class);
        service2.setRef(new DemoServiceImpl());
        service2.setMethods(Collections.singletonList(method));
        service2.setProxy("testproxyfactory");

        delayService.setProvider(provider);
        delayService.setApplication(app);
        delayService.setRegistry(registry);
        delayService.setInterface(DemoService.class);
        delayService.setRef(new DemoServiceImpl());
        delayService.setMethods(Collections.singletonList(method));
        delayService.setDelay(100);

//        ApplicationModel.getConfigManager().clear();
    }

    @AfterEach
    public void tearDown() {
//        ApplicationModel.getConfigManager().clear();
    }

    @Test
    public void testExport() throws Exception {
        service.export();
        try {
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
        }finally {
            service.unexport();
        }
    }

    @Test
    public void testVersionAndGroupConfigFromProvider() {
        //Service no configuration version , the Provider configured.
        service.getProvider().setVersion("1.0.0");
        service.getProvider().setGroup("groupA");
        service.export();

        String serviceVersion = service.getVersion();
        String serviceVersion2 = service.toUrl().getParameter(VERSION_KEY);

        String group = service.getGroup();
        String group2 = service.toUrl().getParameter(GROUP_KEY);

        assertEquals(serviceVersion2, serviceVersion);
        assertEquals(group, group2);
    }

    @Test
    public void testProxy() throws Exception {
        service2.export();
        try {
            assertThat(service2.getExportedUrls(), hasSize(1));
            assertEquals(2, TestProxyFactory.count); // local injvm and registry protocol, so expected is 2
            TestProxyFactory.count = 0;
        }finally {
            service2.unexport();
        }
    }


    @Test
    public void testDelayExport() throws Exception {
        delayService.export();
        try {
            assertTrue(delayService.getExportedUrls().isEmpty());
            //add 300ms to ensure that the delayService has been exported
            TimeUnit.MILLISECONDS.sleep(delayService.getDelay() + 300);
            assertThat(delayService.getExportedUrls(), hasSize(1));
        }finally {
            delayService.unexport();
        }
    }

    @Test
    @Disabled("cannot pass in travis")
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
        ServiceConfig<Greeting> service = new ServiceConfig<Greeting>();
        service.setInterface(Greeting.class.getName());
        service.setRef(Mockito.mock(Greeting.class));
        assertThat(service.getInterfaceClass() == Greeting.class, is(true));
        service = new ServiceConfig<Greeting>();
        service.setRef(Mockito.mock(Greeting.class, withSettings().extraInterfaces(GenericService.class)));
        assertThat(service.getInterfaceClass() == GenericService.class, is(true));
    }

    @Test
    public void testInterface1() throws Exception {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
            service.setInterface(DemoServiceImpl.class);
        });
    }

    @Test
    public void testInterface2() throws Exception {
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
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
        DubboBootstrap.reset();
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
        try {
            Assertions.assertNotNull(service.toUrl().getParameter(APPLICATION_KEY));
            Assertions.assertEquals("app", service.toUrl().getParameter(APPLICATION_KEY));
        }finally {
            service.unexport();
        }
    }
}
