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
package org.apache.dubbo.qos.command.util;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.qos.DemoService;
import org.apache.dubbo.qos.DemoServiceImpl;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for ServiceCheckUtils
 */
class ServiceCheckUtilsTest {

    private final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    @Test
    void testIsRegistered() {
        DemoService demoServiceImpl = new DemoServiceImpl();

        int availablePort = NetUtils.getAvailablePort();

        URL url = URL.valueOf("tri://127.0.0.1:" + availablePort + "/" + DemoService.class.getName());

        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);

        ProviderModel providerModel = new ProviderModel(
            url.getServiceKey(),
            demoServiceImpl,
            serviceDescriptor,
            new ServiceMetadata(), ClassUtils.getClassLoader(DemoService.class));
        repository.registerProvider(providerModel);

        String url1 = "service-discovery-registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider&dubbo=2.0.2&pid=66099&registry=zookeeper&timestamp=1654588337653";
        String url2 = "zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider&dubbo=2.0.2&pid=66099&timestamp=1654588337653";
        providerModel.getStatedUrl().add(new ProviderModel.RegisterStatedURL(url, URL.valueOf(url1), true));
        providerModel.getStatedUrl().add(new ProviderModel.RegisterStatedURL(url, URL.valueOf(url2), false));

        Assertions.assertEquals("zookeeper-A(Y)/zookeeper-I(N)", ServiceCheckUtils.getRegisterStatus(providerModel));
    }

    @Test
    void testGetConsumerAddressNum() {
        ConsumerModel consumerModel = Mockito.mock(ConsumerModel.class);
        ServiceMetadata serviceMetadata = Mockito.mock(ServiceMetadata.class);
        Mockito.when(consumerModel.getServiceMetadata()).thenReturn(serviceMetadata);
        String registry1 = "service-discovery-registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider&dubbo=2.0.2&pid=66099&registry=zookeeper&timestamp=1654588337653";
        String registry2 = "zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider&dubbo=2.0.2&pid=66099&timestamp=1654588337653";
        String registry3 = "nacos://127.0.0.1:8848/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider&dubbo=2.0.2&pid=66099&timestamp=1654588337653";
        Map<Registry, MigrationInvoker<?>> invokerMap = new LinkedHashMap<>();
        {
            Registry registry = Mockito.mock(Registry.class);
            Mockito.when(registry.getUrl()).thenReturn(URL.valueOf(registry1));
            MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
            Mockito.when(migrationInvoker.getMigrationStep()).thenReturn(MigrationStep.FORCE_APPLICATION);
            ClusterInvoker serviceDiscoveryInvoker = Mockito.mock(ClusterInvoker.class);
            Mockito.when(migrationInvoker.getServiceDiscoveryInvoker()).thenReturn(serviceDiscoveryInvoker);
            Directory<?> sdDirectory = Mockito.mock(Directory.class);
            Mockito.when(serviceDiscoveryInvoker.getDirectory()).thenReturn(sdDirectory);
            List sdInvokers = Mockito.mock(List.class);
            Mockito.when(sdDirectory.getAllInvokers()).thenReturn(sdInvokers);
            Mockito.when(sdInvokers.size()).thenReturn(5);
            invokerMap.put(registry, migrationInvoker);
        }

        {
            Registry registry = Mockito.mock(Registry.class);
            Mockito.when(registry.getUrl()).thenReturn(URL.valueOf(registry2));
            MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
            Mockito.when(migrationInvoker.getMigrationStep()).thenReturn(MigrationStep.APPLICATION_FIRST);
            ClusterInvoker serviceDiscoveryInvoker = Mockito.mock(ClusterInvoker.class);
            Mockito.when(migrationInvoker.getServiceDiscoveryInvoker()).thenReturn(serviceDiscoveryInvoker);
            Directory<?> sdDirectory = Mockito.mock(Directory.class);
            Mockito.when(serviceDiscoveryInvoker.getDirectory()).thenReturn(sdDirectory);
            List sdInvokers = Mockito.mock(List.class);
            Mockito.when(sdDirectory.getAllInvokers()).thenReturn(sdInvokers);
            Mockito.when(sdInvokers.size()).thenReturn(0);

            ClusterInvoker invoker = Mockito.mock(ClusterInvoker.class);
            Mockito.when(migrationInvoker.getInvoker()).thenReturn(invoker);
            Directory<?> directory = Mockito.mock(Directory.class);
            Mockito.when(invoker.getDirectory()).thenReturn(directory);
            List invokers = Mockito.mock(List.class);
            Mockito.when(directory.getAllInvokers()).thenReturn(invokers);
            Mockito.when(invokers.size()).thenReturn(10);
            invokerMap.put(registry, migrationInvoker);
        }

        {
            Registry registry = Mockito.mock(Registry.class);
            Mockito.when(registry.getUrl()).thenReturn(URL.valueOf(registry3));
            MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
            Mockito.when(migrationInvoker.getMigrationStep()).thenReturn(MigrationStep.FORCE_INTERFACE);
            ClusterInvoker invoker = Mockito.mock(ClusterInvoker.class);
            Mockito.when(migrationInvoker.getInvoker()).thenReturn(invoker);
            Directory<?> directory = Mockito.mock(Directory.class);
            Mockito.when(invoker.getDirectory()).thenReturn(directory);
            List invokers = Mockito.mock(List.class);
            Mockito.when(directory.getAllInvokers()).thenReturn(invokers);
            Mockito.when(invokers.size()).thenReturn(10);
            invokerMap.put(registry, migrationInvoker);
        }

        Mockito.when(serviceMetadata.getAttribute("currentClusterInvoker")).thenReturn(invokerMap);

        assertEquals("zookeeper-A(5)/zookeeper-AF(I-10,A-0)/nacos-I(10)", ServiceCheckUtils.getConsumerAddressNum(consumerModel));
    }


}