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
package org.apache.dubbo.registry.client.metadata;


import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.integration.DemoService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MetadataUtilsTest {

    @BeforeEach
    public void setup() {
        ApplicationModel.defaultModel().destroy();
    }

    @AfterEach
    public void tearDown() throws IOException {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testComputeKey() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "1");
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(serviceInstance.getServiceName()).thenReturn(DemoService.class.getName());
        when(serviceInstance.getMetadata()).thenReturn(metadata);
        when(serviceInstance.getAddress()).thenReturn("127.0.0.1");

        String expected = "org.apache.dubbo.registry.integration.DemoService##127.0.0.1##1";

        Assertions.assertEquals(expected, MetadataUtils.computeKey(serviceInstance));
    }


    @Test
    public void testCreateMetadataInvoker() {

        Map<String, String> metadata = new HashMap<>();
        metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "1");
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(serviceInstance.getServiceName()).thenReturn(DemoService.class.getName());
        when(serviceInstance.getMetadata()).thenReturn(metadata);
        when(serviceInstance.getAddress()).thenReturn("127.0.0.1");
        when(serviceInstance.getHost()).thenReturn("127.0.0.1");

        String key = "org.apache.dubbo.registry.integration.DemoService##127.0.0.1##1";
        Assertions.assertFalse(MetadataUtils.metadataServiceProxies.containsKey(key));
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setMetadataServicePort(7001);
        applicationConfig.setParameters(parameters);

        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        Protocol protocol = mock(Protocol.class);
        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        Invoker<Object> invoker = mock(Invoker.class);
        MetadataService metadataService = mock(MetadataService.class);

        when(serviceInstance.getApplicationModel()).thenReturn(applicationModel);

        ExtensionLoader<Protocol> protocolExtensionLoader = mock(ExtensionLoader.class);
        when(protocolExtensionLoader.getAdaptiveExtension()).thenReturn(protocol);

        ExtensionLoader<ProxyFactory> proxyFactoryExtensionLoader = mock(ExtensionLoader.class);
        when(proxyFactoryExtensionLoader.getAdaptiveExtension()).thenReturn(proxyFactory);

        when(applicationModel.getExtensionLoader(Protocol.class)).thenReturn(protocolExtensionLoader);
        when(applicationModel.getExtensionLoader(ProxyFactory.class)).thenReturn(proxyFactoryExtensionLoader);

        when(protocol.refer(any(), any())).thenReturn(invoker);
        when(proxyFactory.getProxy(invoker)).thenReturn(metadataService);

        MetadataUtils.getMetadataServiceProxy(serviceInstance);

        Assertions.assertEquals(1, MetadataUtils.getMetadataServiceProxies().size());
        Assertions.assertEquals(1, MetadataUtils.getMetadataServiceInvokers().size());
        Assertions.assertEquals(metadataService, MetadataUtils.getMetadataServiceProxy(serviceInstance));

        MetadataUtils.destroyMetadataServiceProxy(serviceInstance);
        ApplicationModel.defaultModel().destroy();
    }


    @Test
    public void testDestroyMetadataServiceProxy() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "1");
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(serviceInstance.getServiceName()).thenReturn(DemoService.class.getName());
        when(serviceInstance.getMetadata()).thenReturn(metadata);
        when(serviceInstance.getAddress()).thenReturn("127.0.0.1");
        when(serviceInstance.getHost()).thenReturn("127.0.0.1");

        String key = "org.apache.dubbo.registry.integration.DemoService##127.0.0.1##1";
        Assertions.assertFalse(MetadataUtils.metadataServiceProxies.containsKey(key));
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setMetadataServicePort(7001);
        applicationConfig.setParameters(parameters);

        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        when(serviceInstance.getApplicationModel()).thenReturn(applicationModel);

        Protocol protocol = mock(Protocol.class);
        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        Invoker<Object> invoker = mock(Invoker.class);
        MetadataService metadataService = mock(MetadataService.class);

        ExtensionLoader<Protocol> protocolExtensionLoader = mock(ExtensionLoader.class);
        when(protocolExtensionLoader.getAdaptiveExtension()).thenReturn(protocol);

        ExtensionLoader<ProxyFactory> proxyFactoryExtensionLoader = mock(ExtensionLoader.class);
        when(proxyFactoryExtensionLoader.getAdaptiveExtension()).thenReturn(proxyFactory);

        when(applicationModel.getExtensionLoader(Protocol.class)).thenReturn(protocolExtensionLoader);
        when(applicationModel.getExtensionLoader(ProxyFactory.class)).thenReturn(proxyFactoryExtensionLoader);

        when(protocol.refer(any(), any())).thenReturn(invoker);
        when(proxyFactory.getProxy(invoker)).thenReturn(metadataService);

        MetadataUtils.getMetadataServiceProxy(serviceInstance);

        MetadataUtils.destroyMetadataServiceProxy(serviceInstance);

        Assertions.assertEquals(0, MetadataUtils.getMetadataServiceProxies().size());
        Assertions.assertEquals(0, MetadataUtils.getMetadataServiceInvokers().size());
        ApplicationModel.defaultModel().destroy();
    }


}
