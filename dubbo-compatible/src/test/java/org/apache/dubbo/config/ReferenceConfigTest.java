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
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.service.DemoService;
import org.apache.dubbo.service.DemoServiceImpl;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReferenceConfigTest {
    private ApplicationConfig application = new ApplicationConfig();
    private RegistryConfig registry = new RegistryConfig();
    private ProtocolConfig protocol = new ProtocolConfig();

    @BeforeEach
    public void setUp() {
        ApplicationModel.reset();
    }

    @AfterEach
    public void tearDown() {
        ApplicationModel.reset();
    }

    @Test
    public void testInjvm() throws Exception {

        application.setName("test-protocol-random-port");
        registry.setAddress("multicast://224.5.6.7:1234");

        protocol.setName("dubbo");

        ServiceConfig<DemoService> demoService;
        demoService = new ServiceConfig<DemoService>();
        demoService.setInterface(DemoService.class);
        demoService.setRef(new DemoServiceImpl());
        demoService.setApplication(application);
        demoService.setRegistry(registry);
        demoService.setProtocol(protocol);

        ReferenceConfig<DemoService> rc = new ReferenceConfig<DemoService>();
        rc.setApplication(application);
        rc.setRegistry(registry);
        rc.setInterface(DemoService.class.getName());
        rc.setInjvm(false);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance()
                .application(application)
                .registry(registry)
                .protocol(protocol)
                .service(demoService)
                .reference(rc);

        try {
            bootstrap.start();
        } finally {
            bootstrap.stop();
        }
    }
}