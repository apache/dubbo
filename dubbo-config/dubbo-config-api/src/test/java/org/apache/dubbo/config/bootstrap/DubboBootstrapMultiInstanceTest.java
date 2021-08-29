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

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.registrycenter.DefaultSingleRegistryCenter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DubboBootstrapMultiInstanceTest {

    private static DefaultSingleRegistryCenter registryCenter;

    @BeforeAll
    public static void setup() {
        registryCenter = new DefaultSingleRegistryCenter(NetUtils.getAvailablePort());
        registryCenter.startup();
    }

    @AfterAll
    public static void teardown() {
        registryCenter.shutdown();
    }

    @AfterEach
    protected void afterEach() {
        SysProps.clear();
        DubboBootstrap.reset();
    }

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
        configProviderApp(dubboBootstrap1).start();

        // bootstrap2: consumer app
        configConsumerApp(dubboBootstrap2).start();
        testConsumer(dubboBootstrap2);

        DemoService demoServiceFromProvider = dubboBootstrap1.getCache().get(DemoService.class);
        Assertions.assertNull(demoServiceFromProvider);
    }

    @Test
    public void testDefaultProviderApplication() {
        configProviderApp(DubboBootstrap.getInstance()).start();
    }

    @Test
    public void testDefaultConsumerApplication() {
        SysProps.setProperty("dubbo.consumer.check", "false");
        try {
            DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance();
            configConsumerApp(dubboBootstrap).start();
            testConsumer(dubboBootstrap);
        } catch (Exception e) {
            Assertions.assertTrue(e.toString().contains("No provider available from registry"), StringUtils.toString(e));
        }
    }

    @Test
    public void testDefaultMixedApplication() {
        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance();
        dubboBootstrap.application("mixed-app");

        configProviderApp(dubboBootstrap);
        configConsumerApp(dubboBootstrap);
        dubboBootstrap.start();
        testConsumer(dubboBootstrap);
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

    private DubboBootstrap configConsumerApp(DubboBootstrap dubboBootstrap) {
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(DemoService.class);

        if (!dubboBootstrap.getConfigManager().getApplication().isPresent()) {
            dubboBootstrap.application("consumer-app");
        }
        dubboBootstrap.registry(registryCenter.getRegistryConfig())
            .reference(referenceConfig);
        return dubboBootstrap;
    }

    private void testConsumer(DubboBootstrap dubboBootstrap) {
        DemoService demoService = dubboBootstrap.getCache().get(DemoService.class);
        String result = demoService.sayName("dubbo");
        System.out.println("result: " + result);
        Assertions.assertEquals("say:dubbo", result);
    }

    private DubboBootstrap configProviderApp(DubboBootstrap dubboBootstrap) {
        ProtocolConfig protocol1 = new ProtocolConfig();
        protocol1.setName("dubbo");
        protocol1.setPort(2001);

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        if (!dubboBootstrap.getConfigManager().getApplication().isPresent()) {
            dubboBootstrap.application("provider-app");
        }
        dubboBootstrap.registry(registryCenter.getRegistryConfig())
            .protocol(protocol1)
            .service(serviceConfig);
        return dubboBootstrap;
    }

}
