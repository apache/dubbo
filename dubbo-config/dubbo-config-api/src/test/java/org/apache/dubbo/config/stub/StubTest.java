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
package org.apache.dubbo.config.stub;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StubTest {
    private static RegistryConfig registryConfig;

    @BeforeAll
    public static void beforeAll() {
        FrameworkModel.destroyAll();
        registryConfig = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress1());
    }

    @Test
    public void test() {
        DubboBootstrap dubboBootstrap1 = DubboBootstrap.newInstance(new FrameworkModel());
        DubboBootstrap dubboBootstrap2 = DubboBootstrap.newInstance(new FrameworkModel());
        try {

            // bootstrap1: provider app
            configProviderApp(dubboBootstrap1).start();

            // bootstrap2: consumer app
            configConsumerApp(dubboBootstrap2).start();

        } finally {
            dubboBootstrap2.destroy();
            dubboBootstrap1.destroy();
        }

        // verify stub event (onConnect, onDisConnect)
        Assertions.assertTrue(ConsumerDemoServiceLocal.isConnected());
        Assertions.assertTrue(ConsumerDemoServiceLocal.isDisConnected());
        Assertions.assertTrue(DemoServiceImpl.isDisConnected());
        Assertions.assertTrue(DemoServiceImpl.isDisConnected());
    }

    private DubboBootstrap configProviderApp(DubboBootstrap dubboBootstrap) {
        ProtocolConfig protocol1 = new ProtocolConfig();
        protocol1.setName("dubbo");
        protocol1.setPort(20111);

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());
        serviceConfig.setOnconnect("onConnectTest");
        serviceConfig.setOndisconnect("onDisConnectTest");

        if (!dubboBootstrap.getConfigManager().getApplication().isPresent()) {
            dubboBootstrap.application("provider-app");
        }
        dubboBootstrap.registry(registryConfig)
            .protocol(protocol1)
            .service(serviceConfig);
        return dubboBootstrap;
    }

    private DubboBootstrap configConsumerApp(DubboBootstrap dubboBootstrap) {
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setInjvm(false);

        referenceConfig.setOnconnect("onConnectTest");
        referenceConfig.setOndisconnect("onDisConnectTest");
        referenceConfig.setStub(ConsumerDemoServiceLocal.class.getName());

        if (!dubboBootstrap.getConfigManager().getApplication().isPresent()) {
            dubboBootstrap.application("consumer-app");
        }
        dubboBootstrap.registry(registryConfig)
            .reference(referenceConfig);
        return dubboBootstrap;
    }

}
