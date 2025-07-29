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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscoveryTimelineCommandTest {

    private DiscoveryTimelineCommand command;
    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private RegistryManager registryManager;
    private ServiceDiscovery serviceDiscovery;
    private DefaultServiceInstance serviceInstance;
    private FrameworkServiceRepository serviceRepository;
    private ScopeBeanFactory beanFactory;

    @BeforeEach
    public void setUp() {
        frameworkModel = Mockito.mock(FrameworkModel.class);
        applicationModel = Mockito.mock(ApplicationModel.class);
        registryManager = Mockito.mock(RegistryManager.class);
        serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        serviceInstance = Mockito.mock(DefaultServiceInstance.class);
        serviceRepository = Mockito.mock(FrameworkServiceRepository.class);
        beanFactory = Mockito.mock(ScopeBeanFactory.class);

        command = new DiscoveryTimelineCommand(frameworkModel);

        Mockito.when(frameworkModel.getServiceRepository()).thenReturn(serviceRepository);
        Mockito.when(frameworkModel.defaultApplication()).thenReturn(applicationModel);
        Mockito.when(applicationModel.getBeanFactory()).thenReturn(beanFactory);
        Mockito.when(beanFactory.getBean(RegistryManager.class)).thenReturn(registryManager);
        Mockito.when(registryManager.getServiceDiscoveries()).thenReturn(Collections.singletonList(serviceDiscovery));
        Mockito.when(serviceDiscovery.getLocalInstance()).thenReturn(serviceInstance);
        Mockito.when(serviceDiscovery.getUrl()).thenReturn(URL.valueOf("zookeeper://127.0.0.1:2181"));
    }

    @Test
    void testExecuteWithTimestamps() {
        ProviderModel providerModel = Mockito.mock(ProviderModel.class);
        Mockito.when(providerModel.getServiceKey()).thenReturn("org.apache.dubbo.demo.DemoService");
        Mockito.when(serviceRepository.allProviderModels()).thenReturn(Collections.singletonList(providerModel));

        long ts = LocalDateTime.of(2025, 7, 27, 17, 54, 0).toEpochSecond(ZoneOffset.ofHours(5)) * 1000L;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("timestamp", String.valueOf(ts));
        Mockito.when(serviceInstance.getMetadata()).thenReturn(metadata);

        String output = command.execute(new CommandContext("discovery-timeline"), null);
        System.out.println("testExecuteWithTimestamps Output:\n" + output);

        assertTrue(output.contains("Discovery Timeline"));
        assertTrue(output.contains("127.0.0.1:2181"));
        assertTrue(output.contains("Discovered: org.apache.dubbo.demo.DemoService"));
    }

    @Test
    void testExecuteWithoutMetadata() {
        ProviderModel providerModel = Mockito.mock(ProviderModel.class);
        Mockito.when(providerModel.getServiceKey()).thenReturn("org.apache.dubbo.demo.DemoService");
        Mockito.when(serviceRepository.allProviderModels()).thenReturn(Collections.singletonList(providerModel));

        Mockito.when(serviceInstance.getMetadata()).thenReturn(null);

        String output = command.execute(new CommandContext("discovery-timeline"), null);
        System.out.println("testExecuteWithoutMetadata Output:\n" + output);

        assertTrue(output.contains("Unknown"));
        assertTrue(output.contains("Discovered: org.apache.dubbo.demo.DemoService"));
    }

    @Test
    void testExecuteWithPagination() {
        List<ProviderModel> providers = new ArrayList<>();
        long ts = LocalDateTime.of(2025, 7, 27, 17, 54, 0).toEpochSecond(ZoneOffset.ofHours(5)) * 1000L;
        for (int i = 1; i <= 15; i++) {
            ProviderModel model = Mockito.mock(ProviderModel.class);
            String serviceName = "Service" + i;
            Mockito.when(model.getServiceKey()).thenReturn(serviceName);
            providers.add(model);
        }
        Mockito.when(serviceRepository.allProviderModels()).thenReturn(providers);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("timestamp", String.valueOf(ts));
        Mockito.when(serviceInstance.getMetadata()).thenReturn(metadata);

        String output = command.execute(new CommandContext("discovery-timeline"), new String[] {"limit=5", "page=2"});
        System.out.println("testExecuteWithPagination Output:\n" + output);
        System.out.println("Providers mocked: "
                + providers.stream().map(ProviderModel::getServiceKey).collect(Collectors.toList()));
        System.out.println("Output characters:");
        for (int i = 0; i < output.length(); i++) {
            System.out.printf("Index %d: %c (ASCII %d)%n", i, output.charAt(i), (int) output.charAt(i));
        }

        assertTrue(output.contains("Discovery Timeline"));
        assertTrue(output.contains("Discovered: Service6"));
        assertTrue(output.contains("Discovered: Service10"));
    }

    @Test
    void testExecuteWithServiceFilter() {
        ProviderModel providerModel = Mockito.mock(ProviderModel.class);
        Mockito.when(providerModel.getServiceKey()).thenReturn("org.apache.dubbo.MyService");
        Mockito.when(serviceRepository.allProviderModels()).thenReturn(Collections.singletonList(providerModel));
        Mockito.when(serviceInstance.getMetadata()).thenReturn(new HashMap<>());

        String output = command.execute(new CommandContext("discovery-timeline"), new String[] {"service=MyService"});
        System.out.println("testExecuteWithServiceFilter Output:\n" + output);

        assertTrue(output.contains("Discovered: org.apache.dubbo.MyService"));
    }

    @Test
    void testExecuteWithRegistryFilterMismatch() {
        ProviderModel providerModel = Mockito.mock(ProviderModel.class);
        Mockito.when(providerModel.getServiceKey()).thenReturn("org.apache.dubbo.MyService");
        Mockito.when(serviceRepository.allProviderModels()).thenReturn(Collections.singletonList(providerModel));
        Mockito.when(serviceInstance.getMetadata()).thenReturn(new HashMap<>());

        String output = command.execute(new CommandContext("discovery-timeline"), new String[] {"registry=nacos"});
        System.out.println("testExecuteWithRegistryFilterMismatch Output:\n" + output);

        assertTrue(output.contains("Error: No services discovered."));
    }
}
