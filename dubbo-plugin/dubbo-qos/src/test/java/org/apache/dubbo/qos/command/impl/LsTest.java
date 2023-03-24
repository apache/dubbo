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

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.qos.DemoService;
import org.apache.dubbo.qos.DemoServiceImpl;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.rpc.model.AsyncMethodInfo;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class LsTest {
    private FrameworkModel frameworkModel;
    private ModuleServiceRepository repository;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
        repository = frameworkModel.newApplication().getDefaultModule().getServiceRepository();
        registerProvider();
        registerConsumer();
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    void testExecute() {
        Ls ls = new Ls(frameworkModel);
        String result = ls.execute(Mockito.mock(CommandContext.class), new String[0]);
        System.out.println(result);
        /**
         As Provider side:
         +--------------------------------+---+
         |      Provider Service Name     |PUB|
         +--------------------------------+---+
         |org.apache.dubbo.qos.DemoService| N |
         +--------------------------------+---+
         As Consumer side:
         +--------------------------------+---+
         |      Consumer Service Name     |NUM|
         +--------------------------------+---+
         |org.apache.dubbo.qos.DemoService| 0 |
         +--------------------------------+---+
         */
    }

    private void registerProvider() {
        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setServiceKey(DemoService.class.getName());
        ProviderModel providerModel = new ProviderModel(
            DemoService.class.getName(),
            new DemoServiceImpl(),
            serviceDescriptor,
            null,
            serviceMetadata, ClassUtils.getClassLoader(DemoService.class));
        repository.registerProvider(providerModel);
    }

    private void registerConsumer() {
        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInterface(DemoService.class);
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setServiceKey(DemoService.class.getName());
        Map<String, AsyncMethodInfo> methodConfigs = new HashMap<>();
        ConsumerModel consumerModel = new ConsumerModel(
            serviceMetadata.getServiceKey(), null, serviceDescriptor,
            serviceMetadata, methodConfigs, referenceConfig.getInterfaceClassLoader());
        repository.registerConsumer(consumerModel);
    }
}
