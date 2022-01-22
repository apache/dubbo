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

import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.DemoServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * {@link ModuleServiceRepository}
 */
public class ModuleServiceRepositoryTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
        applicationModel = new ApplicationModel(frameworkModel);
        moduleModel = new ModuleModel(applicationModel);
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    public void test() {
        ModuleServiceRepository moduleServiceRepository = new ModuleServiceRepository(moduleModel);
        Assertions.assertEquals(moduleServiceRepository.getModuleModel(), moduleModel);
        ModuleServiceRepository repository = moduleModel.getServiceRepository();

        // 1.test service
        ServiceMetadata serviceMetadata = new ServiceMetadata(DemoService.class.getName(), null, null, DemoService.class);
        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);

        ServiceDescriptor lookupServiceResult = repository.lookupService(DemoService.class.getName());
        Assertions.assertEquals(lookupServiceResult, serviceDescriptor);

        List<ServiceDescriptor> allServices = repository.getAllServices();
        Assertions.assertEquals(allServices.size(), 1);
        Assertions.assertEquals(allServices.get(0), serviceDescriptor);

        ServiceDescriptor serviceDescriptor1 = repository.registerService(DemoService.class.getSimpleName(), DemoService.class);
        Assertions.assertEquals(serviceDescriptor1, serviceDescriptor);


        // 2.test consumerModule
        ConsumerModel consumerModel = new ConsumerModel(
            serviceMetadata.getServiceKey(), new DemoServiceImpl(), serviceDescriptor, null,
            moduleModel, serviceMetadata, null);
        repository.registerConsumer(consumerModel);

        List<ConsumerModel> allReferredServices = repository.getReferredServices();
        Assertions.assertEquals(allReferredServices.size(), 1);
        Assertions.assertEquals(allReferredServices.get(0), consumerModel);

        List<ConsumerModel> referredServices = repository.lookupReferredServices(DemoService.class.getName());
        Assertions.assertEquals(referredServices.size(), 1);
        Assertions.assertEquals(referredServices.get(0), consumerModel);

        ConsumerModel referredService = repository.lookupReferredService(DemoService.class.getName());
        Assertions.assertEquals(referredService, consumerModel);

        // 3.test providerModel
        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(),
            new DemoServiceImpl(),
            serviceDescriptor,
            null,
            moduleModel,
            serviceMetadata);
        repository.registerProvider(providerModel);
        List<ProviderModel> allExportedServices = repository.getExportedServices();
        Assertions.assertEquals(allExportedServices.size(), 1);
        Assertions.assertEquals(allExportedServices.get(0), providerModel);

        ProviderModel exportedService = repository.lookupExportedService(DemoService.class.getName());
        Assertions.assertEquals(exportedService, providerModel);

        List<ProviderModel> providerModels = frameworkModel.getServiceRepository().allProviderModels();
        Assertions.assertEquals(providerModels.size(), 1);
        Assertions.assertEquals(providerModels.get(0), providerModel);

        // 4.test destroy
        repository.destroy();
        Assertions.assertTrue(repository.getAllServices().isEmpty());
        Assertions.assertTrue(repository.getReferredServices().isEmpty());
        Assertions.assertTrue(repository.getExportedServices().isEmpty());
        Assertions.assertTrue(frameworkModel.getServiceRepository().allProviderModels().isEmpty());
    }
}
