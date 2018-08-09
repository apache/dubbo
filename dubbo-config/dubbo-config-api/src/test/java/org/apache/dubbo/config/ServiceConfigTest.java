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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.mock.TestProxyFactory;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.config.mock.MockProtocol2;
import org.apache.dubbo.config.mock.MockRegistryFactory2;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.apache.dubbo.common.Constants.GENERIC_SERIALIZATION_BEAN;
import static org.apache.dubbo.common.Constants.GENERIC_SERIALIZATION_DEFAULT;
import static org.apache.dubbo.common.Constants.GENERIC_SERIALIZATION_NATIVE_JAVA;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.withSettings;

public class ServiceConfigTest {
    private Protocol protocolDelegate = Mockito.mock(Protocol.class);
    private Registry registryDelegate = Mockito.mock(Registry.class);
    private Exporter exporter = Mockito.mock(Exporter.class);
    private ServiceConfig<DemoServiceImpl> service = new ServiceConfig<DemoServiceImpl>();
    private ServiceConfig<DemoServiceImpl> service2 = new ServiceConfig<DemoServiceImpl>();


    @Before
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
    }

    @Test
    public void testExport() throws Exception {
        service.export();

        assertThat(service.getExportedUrls(), hasSize(1));
        URL url = service.toUrl();
        assertThat(url.getProtocol(), equalTo("mockprotocol2"));
        assertThat(url.getPath(), equalTo(DemoService.class.getName()));
        assertThat(url.getParameters(), hasEntry(Constants.ANYHOST_KEY, "true"));
        assertThat(url.getParameters(), hasEntry(Constants.APPLICATION_KEY, "app"));
        assertThat(url.getParameters(), hasKey(Constants.BIND_IP_KEY));
        assertThat(url.getParameters(), hasKey(Constants.BIND_PORT_KEY));
        assertThat(url.getParameters(), hasEntry(Constants.DEFAULT_KEY + "." + Constants.EXPORT_KEY, "true"));
        assertThat(url.getParameters(), hasEntry(Constants.EXPORT_KEY, "true"));
        assertThat(url.getParameters(), hasEntry("echo.0.callback", "false"));
        assertThat(url.getParameters(), hasEntry(Constants.GENERIC_KEY, "false"));
        assertThat(url.getParameters(), hasEntry(Constants.INTERFACE_KEY, DemoService.class.getName()));
        assertThat(url.getParameters(), hasKey(Constants.METHODS_KEY));
        assertThat(url.getParameters().get(Constants.METHODS_KEY), containsString("echo"));
        assertThat(url.getParameters(), hasEntry(Constants.SIDE_KEY, Constants.PROVIDER));
        Mockito.verify(protocolDelegate).export(Mockito.any(Invoker.class));
    }

    @Test
    public void testProxy() throws Exception {
        service2.export();

        assertThat(service2.getExportedUrls(), hasSize(1));
        assertEquals(2, TestProxyFactory.count); // local injvm and registry protocol, so expected is 2
    }

    @Test
    @Ignore("cannot pass in travis")
    public void testUnexport() throws Exception {
        System.setProperty(Constants.SHUTDOWN_WAIT_KEY, "0");
        try {
            service.export();
            service.unexport();
            Thread.sleep(1000);
            Mockito.verify(exporter, Mockito.atLeastOnce()).unexport();
        } finally {
            System.clearProperty(Constants.SHUTDOWN_TIMEOUT_KEY);
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

    @Test(expected = IllegalStateException.class)
    public void testInterface1() throws Exception {
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setInterface(DemoServiceImpl.class);
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

    @Test(expected = IllegalArgumentException.class)
    public void testGeneric2() throws Exception {
        ServiceConfig service = new ServiceConfig();
        service.setGeneric("illegal");
    }

    @Test
    public void testUniqueServiceName() throws Exception {
        ServiceConfig<Greeting> service = new ServiceConfig<Greeting>();
        service.setGroup("dubbo");
        service.setInterface(Greeting.class);
        service.setVersion("1.0.0");
        assertThat(service.getUniqueServiceName(), equalTo("dubbo/" + Greeting.class.getName() + ":1.0.0"));
    }
}
