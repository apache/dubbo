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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.qos.DemoService;
import org.apache.dubbo.qos.DemoServiceImpl;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

}
