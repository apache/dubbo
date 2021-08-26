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
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DubboBootstrapMultiInstanceTest {

    @Test
    public void testIsolatedApplications() {

        DubboBootstrap dubboBootstrap1 = DubboBootstrap.newInstance();
        DubboBootstrap dubboBootstrap2 = DubboBootstrap.newInstance();
        ApplicationModel applicationModel1 = dubboBootstrap1.getApplicationModel();
        ApplicationModel applicationModel2 = dubboBootstrap2.getApplicationModel();
        Assertions.assertNotSame(applicationModel1, applicationModel2);
        Assertions.assertNotSame(applicationModel1.getFrameworkModel(), applicationModel2.getFrameworkModel());
        Assertions.assertNotSame(dubboBootstrap1.getConfigManager(), dubboBootstrap2.getConfigManager());

        // bootstrap1: provider app
        RegistryConfig registry1 = new RegistryConfig();
        registry1.setAddress("zookeeper://localhost:2181");

        ProtocolConfig protocol1 = new ProtocolConfig();
        protocol1.setName("dubbo");
        protocol1.setPort(2001);

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        dubboBootstrap1.application("provider-app")
            .registry(registry1)
            .protocol(protocol1)
            .service(serviceConfig)
            .start();


        // bootstrap2: consumer app
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(DemoService.class);

        dubboBootstrap2.application("consumer-app")
            .registry(new RegistryConfig("zookeeper://localhost:2181"))
            .reference(referenceConfig)
            .start();

        DemoService demoServiceRefer = dubboBootstrap2.getCache().get(DemoService.class);
        String result = demoServiceRefer.sayName("dubbo");
        System.out.println("result: " + result);
    }

    @Test
    public void testSharedApplications() {

        FrameworkModel frameworkModel = new FrameworkModel();
        DubboBootstrap dubboBootstrap1 = DubboBootstrap.newInstance(frameworkModel);
        DubboBootstrap dubboBootstrap2 = DubboBootstrap.newInstance(frameworkModel);
        ApplicationModel applicationModel1 = dubboBootstrap1.getApplicationModel();
        ApplicationModel applicationModel2 = dubboBootstrap2.getApplicationModel();
        Assertions.assertNotSame(applicationModel1, applicationModel2);
        Assertions.assertSame(applicationModel1.getFrameworkModel(), applicationModel2.getFrameworkModel());
        Assertions.assertNotSame(dubboBootstrap1.getConfigManager(), dubboBootstrap2.getConfigManager());

    }
}
