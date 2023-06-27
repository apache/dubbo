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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.DemoServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.common.BaseServiceMetadata.interfaceFromServiceKey;
import static org.apache.dubbo.common.BaseServiceMetadata.versionFromServiceKey;

/**
 * {@link FrameworkServiceRepository}
 */
class FrameworkServiceRepositoryTest {
    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
        applicationModel = frameworkModel.newApplication();
        moduleModel = applicationModel.newModule();
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    void test() {
        FrameworkServiceRepository frameworkServiceRepository = frameworkModel.getServiceRepository();
        ModuleServiceRepository moduleServiceRepository = moduleModel.getServiceRepository();

        ServiceMetadata serviceMetadata = new ServiceMetadata(DemoService.class.getName(), "GROUP", "1.0.0", DemoService.class);
        ServiceDescriptor serviceDescriptor = moduleServiceRepository.registerService(DemoService.class);
        String serviceKey = serviceMetadata.getServiceKey();
        ProviderModel providerModel = new ProviderModel(serviceKey,
            new DemoServiceImpl(),
            serviceDescriptor,
            moduleModel,
            serviceMetadata, ClassUtils.getClassLoader(DemoService.class));
        frameworkServiceRepository.registerProvider(providerModel);

        ProviderModel lookupExportedService = frameworkServiceRepository.lookupExportedService(serviceKey);
        Assertions.assertEquals(lookupExportedService, providerModel);

        List<ProviderModel> allProviderModels = frameworkServiceRepository.allProviderModels();
        Assertions.assertEquals(allProviderModels.size(), 1);
        Assertions.assertEquals(allProviderModels.get(0), providerModel);

        String keyWithoutGroup = keyWithoutGroup(serviceKey);
        ProviderModel exportedServiceWithoutGroup = frameworkServiceRepository.lookupExportedServiceWithoutGroup(keyWithoutGroup);
        Assertions.assertEquals(exportedServiceWithoutGroup, providerModel);

        List<ProviderModel> providerModels = frameworkServiceRepository.lookupExportedServicesWithoutGroup(keyWithoutGroup);
        Assertions.assertEquals(providerModels.size(), 1);
        Assertions.assertEquals(providerModels.get(0), providerModel);

        ConsumerModel consumerModel = new ConsumerModel(
            serviceMetadata.getServiceKey(), new DemoServiceImpl(), serviceDescriptor,
            moduleModel, serviceMetadata, null, ClassUtils.getClassLoader(DemoService.class));
        moduleServiceRepository.registerConsumer(consumerModel);
        List<ConsumerModel> consumerModels = frameworkServiceRepository.allConsumerModels();
        Assertions.assertEquals(consumerModels.size(), 1);
        Assertions.assertEquals(consumerModels.get(0), consumerModel);

        frameworkServiceRepository.unregisterProvider(providerModel);
        Assertions.assertNull(frameworkServiceRepository.lookupExportedService(serviceKey));
        Assertions.assertNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(keyWithoutGroup));

    }

    private static String keyWithoutGroup(String serviceKey) {
        String interfaceName = interfaceFromServiceKey(serviceKey);
        String version = versionFromServiceKey(serviceKey);
        if (StringUtils.isEmpty(version)) {
            return interfaceName;
        }
        return interfaceName + ":" + version;
    }
}
