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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeterImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the RequestMapping registration process.
 */
public class RequestMappingRegisterTest {
    ApplicationModel applicationModel = ApplicationModel.defaultModel();
    Invoker<IGreeter> invoker = null;

    /**
     * Setup method, initializes the testing environment.
     * Registers a service provider and creates an Invoker instance for subsequent tests.
     */
    @BeforeEach
    public void setup() {
        // Initialize the service implementation
        IGreeter serviceImpl = new IGreeterImpl();

        // Select an available port
        int availablePort = NetUtils.getAvailablePort();

        // Construct the provider's URL
        URL providerUrl = URL.valueOf("http://127.0.0.1:" + availablePort + "/" + IGreeter.class.getName());

        // Register the service
        ModuleServiceRepository serviceRepository =
                applicationModel.getDefaultModule().getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);

        // Construct and register the provider model
        ProviderModel providerModel = new ProviderModel(
                providerUrl.getServiceKey(),
                serviceImpl,
                serviceDescriptor,
                new ServiceMetadata(),
                ClassUtils.getClassLoader(IGreeter.class));
        serviceRepository.registerProvider(providerModel);
        providerUrl = providerUrl.setServiceModel(providerModel);

        // Initialize the protocol and proxy factory
        Protocol protocol = new TripleProtocol(providerUrl.getOrDefaultFrameworkModel());
        ProxyFactory proxyFactory =
                applicationModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        // Create and export the Invoker
        invoker = proxyFactory.getInvoker(serviceImpl, IGreeter.class, providerUrl);
        protocol.export(invoker);
    }

    /**
     * Tests whether the service lookup mechanism is functioning properly.
     * Ensures that the DefaultRequestMappingRegistry instance can be obtained.
     */
    @Test
    public void testServiceLookup() {
        // Obtain the DefaultRequestMappingRegistry instance
        DefaultRequestMappingRegistry registry =
                applicationModel.getFrameworkModel().getBeanFactory().getBean(DefaultRequestMappingRegistry.class);
        assertNotNull(registry, "The DefaultRequestMappingRegistry should not be null.");
    }
}
