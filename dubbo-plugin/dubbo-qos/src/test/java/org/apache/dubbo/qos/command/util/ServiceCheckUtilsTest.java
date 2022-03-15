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


import com.google.common.collect.Lists;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.qos.DemoService;
import org.apache.dubbo.qos.DemoServiceImpl;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
import org.apache.dubbo.registry.support.RegistryManager;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Test for ServiceCheckUtils
 */
public class ServiceCheckUtilsTest {

    private final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    @Test
    public void testIsRegistered() {
        DemoService demoServiceImpl = new DemoServiceImpl();

        int availablePort = NetUtils.getAvailablePort();

        URL url = URL.valueOf("tri://127.0.0.1:" + availablePort + "/" + DemoService.class.getName());

        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);

        ProviderModel providerModel = new ProviderModel(
            url.getServiceKey(),
            demoServiceImpl,
            serviceDescriptor,
            null,
            new ServiceMetadata());
        repository.registerProvider(providerModel);

        boolean registered = ServiceCheckUtils.isRegistered(providerModel);
        assertFalse(registered);
    }

    private static final ConsumerModel consumerModel;

    static {
        consumerModel = Mockito.mock(ConsumerModel.class);
        ServiceDiscoveryRegistry serviceDiscoveryRegistry = Mockito.mock(ServiceDiscoveryRegistry.class);
        Collection<Registry> registries = Lists.newArrayList(serviceDiscoveryRegistry);

        ModuleModel moduleModel = Mockito.mock(ModuleModel.class);
        ApplicationModel applicationModel = Mockito.mock(ApplicationModel.class);
        ScopeBeanFactory scopeBeanFactory = Mockito.mock(ScopeBeanFactory.class);
        RegistryManager registryManager = Mockito.mock(RegistryManager.class);
        when(applicationModel.getBeanFactory()).thenReturn(scopeBeanFactory);
        when(scopeBeanFactory.getBean(RegistryManager.class)).thenReturn(registryManager);
        when(moduleModel.getApplicationModel()).thenReturn(applicationModel);
        when(consumerModel.getModuleModel()).thenReturn(moduleModel);

        when(registryManager.getRegistries()).thenReturn(registries);
    }

    @Test
    public void testGetConsumerAddressNum() {
        int consumerAddressNum = ServiceCheckUtils.getConsumerAddressNum(consumerModel);
        assertEquals(0, consumerAddressNum);
    }


}
