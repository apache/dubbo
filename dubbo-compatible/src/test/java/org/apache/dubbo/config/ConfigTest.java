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

package org.apache.dubbo.config;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.service.DemoService;
import org.apache.dubbo.service.DemoServiceImpl;

import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigTest {
    private com.alibaba.dubbo.config.ApplicationConfig applicationConfig = new com.alibaba.dubbo.config.ApplicationConfig("first-dubbo-test");
    private com.alibaba.dubbo.config.RegistryConfig registryConfig = new com.alibaba.dubbo.config.RegistryConfig("multicast://224.5.6.7:1234");

    @AfterEach
    public void tearDown() {
        DubboBootstrap.reset();
    }

    @BeforeEach
    public void setup() {
        // In IDE env, make sure adding the following argument to VM options
        System.setProperty("java.net.preferIPv4Stack", "true");
        DubboBootstrap.reset();
    }

    @Test
    public void testConfig() {
        com.alibaba.dubbo.config.ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setApplication(applicationConfig);
        service.setRegistry(registryConfig);
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        com.alibaba.dubbo.config.ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setApplication(applicationConfig);
        reference.setRegistry(registryConfig);
        reference.setInterface(DemoService.class);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance()
                .application(applicationConfig)
                .registry(registryConfig)
                .service(service)
                .reference(reference)
                .start();

        DemoService demoService = bootstrap.getCache().get(reference);
        String message = demoService.sayHello("dubbo");
        Assertions.assertEquals("hello dubbo", message);
    }
}
