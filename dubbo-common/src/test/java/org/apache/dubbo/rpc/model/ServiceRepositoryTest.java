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
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.DemoServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * {@link ServiceRepository}
 */
class ServiceRepositoryTest {
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
        // verify BuiltinService
        Set<BuiltinServiceDetector> builtinServices
            = applicationModel.getExtensionLoader(BuiltinServiceDetector.class).getSupportedExtensionInstances();
        ModuleServiceRepository moduleServiceRepository = applicationModel.getInternalModule().getServiceRepository();
        List<ServiceDescriptor> allServices = moduleServiceRepository.getAllServices();
        Assertions.assertEquals(allServices.size(), builtinServices.size());

        ModuleServiceRepository repository = moduleModel.getServiceRepository();
        ServiceMetadata serviceMetadata = new ServiceMetadata(DemoService.class.getName(), null, null, DemoService.class);
        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);

        // registerConsumer
        ConsumerModel consumerModel = new ConsumerModel(
            serviceMetadata.getServiceKey(), new DemoServiceImpl(), serviceDescriptor,
            moduleModel, serviceMetadata, null, ClassUtils.getClassLoader(DemoService.class));
        repository.registerConsumer(consumerModel);

        // registerProvider
        ProviderModel providerModel = new ProviderModel(DemoService.class.getName(),
            new DemoServiceImpl(),
            serviceDescriptor,
            moduleModel,
            serviceMetadata, ClassUtils.getClassLoader(DemoService.class));
        repository.registerProvider(providerModel);

        // verify allProviderModels, allConsumerModels
        ServiceRepository serviceRepository = applicationModel.getApplicationServiceRepository();
        Collection<ProviderModel> providerModels = serviceRepository.allProviderModels();
        Assertions.assertEquals(providerModels.size(), 1);
        Assertions.assertTrue(providerModels.contains(providerModel));

        Collection<ConsumerModel> consumerModels = serviceRepository.allConsumerModels();
        Assertions.assertEquals(consumerModels.size(), 1);
        Assertions.assertTrue(consumerModels.contains(consumerModel));

    }

}
