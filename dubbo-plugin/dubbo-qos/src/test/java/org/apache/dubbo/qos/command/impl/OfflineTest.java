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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.qos.DemoService;
import org.apache.dubbo.qos.DemoServiceImpl;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.mockito.Mockito.mock;

/**
 * {@link BaseOffline}
 * {@link Offline}
 * {@link OfflineApp}
 * {@link OfflineInterface}
 */
class OfflineTest {
    private FrameworkModel frameworkModel;
    private ModuleServiceRepository repository;
    private ProviderModel.RegisterStatedURL registerStatedURL;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
        repository = frameworkModel.newApplication().getDefaultModule().getServiceRepository();
        registerProvider();
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    void testExecute() {
        Offline offline = new Offline(frameworkModel);
        String result = offline.execute(mock(CommandContext.class), new String[]{DemoService.class.getName()});
        Assertions.assertEquals(result, "OK");
        Assertions.assertFalse(registerStatedURL.isRegistered());

        OfflineInterface offlineInterface = new OfflineInterface(frameworkModel);
        registerStatedURL.setRegistered(true);
        result = offlineInterface.execute(mock(CommandContext.class), new String[]{DemoService.class.getName()});
        Assertions.assertEquals(result, "OK");
        Assertions.assertFalse(registerStatedURL.isRegistered());

        registerStatedURL.setRegistered(true);
        registerStatedURL.setRegistryUrl(URL.valueOf("test://127.0.0.1:2181/" + RegistryService.class.getName())
            .addParameter(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE));
        OfflineApp offlineApp = new OfflineApp(frameworkModel);
        result = offlineApp.execute(mock(CommandContext.class), new String[]{DemoService.class.getName()});
        Assertions.assertEquals(result, "OK");
        Assertions.assertFalse(registerStatedURL.isRegistered());

    }

    private void registerProvider() {
        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setServiceKey(DemoService.class.getName());
        ProviderModel providerModel = new ProviderModel(
            DemoService.class.getName(),
            new DemoServiceImpl(),
            serviceDescriptor,
            serviceMetadata, ClassUtils.getClassLoader(DemoService.class));
        registerStatedURL = new ProviderModel.RegisterStatedURL(
            URL.valueOf("dubbo://127.0.0.1:20880/" + DemoService.class.getName()),
            URL.valueOf("test://127.0.0.1:2181/" + RegistryService.class.getName()),
            true);
        providerModel.addStatedUrl(registerStatedURL
        );
        repository.registerProvider(providerModel);
    }
}
