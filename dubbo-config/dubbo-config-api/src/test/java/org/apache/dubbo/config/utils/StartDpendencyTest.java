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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


@SuppressWarnings({"rawtypes", "unchecked"})
public class StartDpendencyTest {

    @Test
    public void testCheckStartDependency_Consumer_First() {
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        RegistryConfig registryConfig = new RegistryConfig();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("test-check-start-dependency-consumer-first");

        registryConfig.setAddress("multicast://224.5.6.7:1234");
        registryConfig.setTimeout(6000);

        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setCheck(true);
        referenceConfig.setRegistry(registryConfig);
        referenceConfig.setApplication(applicationConfig);

        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        Assertions.assertThrows(NullPointerException.class, referenceConfig::get);

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            serviceConfig.export();

            DemoService demoService = referenceConfig.get();

            Assertions.assertNotNull(demoService.sayName(Mockito.anyString()));
        } finally {
            System.clearProperty("java.net.preferIPv4Stack");
            referenceConfig.destroy();
            serviceConfig.unexport();
        }

    }

    @Test
    public void testCheckStartDependency_Provider_First() {
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        RegistryConfig registryConfig = new RegistryConfig();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("test-check-start-dependency-provider-first");

        registryConfig.setAddress("multicast://224.5.6.7:1234");

        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setCheck(true);
        referenceConfig.setRegistry(registryConfig);
        referenceConfig.setApplication(applicationConfig);

        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        serviceConfig.export();
        DemoService demoService = referenceConfig.get();

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            Assertions.assertNotNull(demoService.sayName(Mockito.anyString()));
        } finally {
            System.clearProperty("java.net.preferIPv4Stack");
            referenceConfig.destroy();
            serviceConfig.unexport();
        }
    }
}
