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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.integration.DemoService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CONSUMER_URL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceDiscoveryRegistryDirectoryTest {
    private static final String TEST_PROTOCOL = "mock";

    @AfterEach
    void tearDown() {
        FrameworkModel.destroyAll();
    }

    @Test
    void testNotifyDeduplicatesInstanceUrlsBeforeRefer() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ModuleModel moduleModel = applicationModel.getDefaultModule();
        ServiceDiscoveryRegistryDirectory<DemoService> directory =
                new ServiceDiscoveryRegistryDirectory<>(DemoService.class, createDirectoryUrl(moduleModel));

        @SuppressWarnings("unchecked")
        RouterChain<DemoService> routerChain = mock(RouterChain.class);
        doAnswer(invocation -> {
                    invocation.getArgument(1, Runnable.class).run();
                    return null;
                })
                .when(routerChain)
                .setInvokers(any(BitList.class), any(Runnable.class));
        directory.setRouterChain(routerChain);

        Protocol protocol = mock(Protocol.class);
        @SuppressWarnings("unchecked")
        Invoker<DemoService> invoker = mock(Invoker.class);
        directory.setProtocol(protocol);

        InstanceAddressURL instanceUrl =
                createProviderInstance(applicationModel).toURL(TEST_PROTOCOL);
        when(invoker.getUrl()).thenReturn(instanceUrl);
        when(protocol.refer(eq(DemoService.class), any(InstanceAddressURL.class)))
                .thenReturn(invoker);

        directory.notify(Arrays.asList(instanceUrl, instanceUrl));

        verify(protocol, times(1)).refer(eq(DemoService.class), any(InstanceAddressURL.class));
        assertEquals(1, directory.getInvokers().size());
    }

    private URL createDirectoryUrl(ModuleModel moduleModel) {
        Map<String, String> params = new HashMap<>();
        params.put(INTERFACE_KEY, DemoService.class.getName());
        params.put(PROTOCOL_KEY, TEST_PROTOCOL);

        URL registryUrl = new ServiceConfigURL(
                "registry", "127.0.0.1", 2181, "org.apache.dubbo.registry.RegistryService", params);
        URL consumerUrl = URL.valueOf("consumer://127.0.0.1/" + DemoService.class.getName() + "?interface="
                        + DemoService.class.getName() + "&check=false&protocol=" + TEST_PROTOCOL)
                .setScopeModel(moduleModel);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(REFER_KEY, Collections.emptyMap());
        attributes.put(CONSUMER_URL_KEY, consumerUrl);
        return registryUrl.addAttributes(attributes).setScopeModel(moduleModel);
    }

    private DefaultServiceInstance createProviderInstance(ApplicationModel applicationModel) {
        DefaultServiceInstance instance = new DefaultServiceInstance();
        instance.setServiceName("demo-provider");
        instance.setHost("127.0.0.1");
        instance.setPort(20880);
        instance.setEnabled(true);
        instance.setHealthy(true);
        instance.setApplicationModel(applicationModel);
        instance.setMetadata(new HashMap<>());

        MetadataInfo.ServiceInfo serviceInfo = new MetadataInfo.ServiceInfo(
                DemoService.class.getName(),
                null,
                null,
                TEST_PROTOCOL,
                20880,
                DemoService.class.getName(),
                new HashMap<>());
        MetadataInfo metadataInfo = new MetadataInfo(
                "demo-provider",
                "rev-1",
                new ConcurrentHashMap<>(Collections.singletonMap(serviceInfo.getMatchKey(), serviceInfo)));
        instance.setServiceMetadata(metadataInfo);
        return instance;
    }
}
