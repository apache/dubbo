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

import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.mock.GreetingLocal2;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.registrycenter.RegistryCenter;
import org.apache.dubbo.registrycenter.ZookeeperSingleRegistryCenter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;

public class DubboBootstrapMultiInstanceTest {

    private static ZookeeperSingleRegistryCenter registryCenter;

    private static RegistryConfig registryConfig;

    @BeforeEach
    public void setup() {
        registryCenter = new ZookeeperSingleRegistryCenter(NetUtils.getAvailablePort());
        registryCenter.startup();
        RegistryCenter.Instance instance = registryCenter.getRegistryCenterInstance().get(0);
        registryConfig = new RegistryConfig(String.format("%s://%s:%s",
            instance.getType(),
            instance.getHostname(),
            instance.getPort()));

    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
        DubboBootstrap.reset();
        registryCenter.shutdown();
    }

    @Test
    public void testIsolatedApplications() {

        DubboBootstrap dubboBootstrap1 = DubboBootstrap.newInstance(new FrameworkModel());
        DubboBootstrap dubboBootstrap2 = DubboBootstrap.newInstance(new FrameworkModel());
        try {
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
        } finally {
            dubboBootstrap2.destroy();
            dubboBootstrap1.destroy();
        }
    }

    @Test
    public void testDefaultProviderApplication() {
        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance();
        try {
            configProviderApp(dubboBootstrap).start();
        } finally {
            dubboBootstrap.destroy();
            DubboBootstrap.reset();
        }
    }

    @Test
    public void testDefaultConsumerApplication() {
        SysProps.setProperty("dubbo.consumer.check", "false");
        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance();
        try {
            configConsumerApp(dubboBootstrap).start();
            testConsumer(dubboBootstrap);
        } catch (Exception e) {
            Assertions.assertTrue(e.toString().contains("No provider available"), StringUtils.toString(e));
        } finally {
            dubboBootstrap.destroy();
            DubboBootstrap.reset();
            SysProps.clear();
        }
    }

    @Test
    public void testDefaultMixedApplication() {
        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance();
        try {
            dubboBootstrap.application("mixed-app");
            configProviderApp(dubboBootstrap);
            configConsumerApp(dubboBootstrap);
            dubboBootstrap.start();

            testConsumer(dubboBootstrap);
        } finally {
            dubboBootstrap.destroy();
            DubboBootstrap.reset();
        }
    }

    @Test
    public void testSharedApplications() {

        FrameworkModel frameworkModel = new FrameworkModel();
        DubboBootstrap dubboBootstrap1 = DubboBootstrap.newInstance(frameworkModel);
        DubboBootstrap dubboBootstrap2 = DubboBootstrap.newInstance(frameworkModel);
        try {
            ApplicationModel applicationModel1 = dubboBootstrap1.getApplicationModel();
            ApplicationModel applicationModel2 = dubboBootstrap2.getApplicationModel();
            Assertions.assertNotSame(applicationModel1, applicationModel2);
            Assertions.assertSame(applicationModel1.getFrameworkModel(), applicationModel2.getFrameworkModel());
            Assertions.assertNotSame(dubboBootstrap1.getConfigManager(), dubboBootstrap2.getConfigManager());

            configProviderApp(dubboBootstrap1).start();
            configConsumerApp(dubboBootstrap2).start();
            testConsumer(dubboBootstrap2);
        } finally {
            dubboBootstrap1.destroy();
            dubboBootstrap2.destroy();
        }
    }

    @Test
    public void testMultiModuleApplication() throws InterruptedException {

        SysProps.setProperty(METADATA_PUBLISH_DELAY_KEY, "1");
        String version1 = "1.0";
        String version2 = "2.0";
        String version3 = "3.0";

        DubboBootstrap providerBootstrap = null;
        DubboBootstrap consumerBootstrap = null;

        try {
            // provider app
            providerBootstrap = DubboBootstrap.newInstance();

            ServiceConfig serviceConfig1 = new ServiceConfig();
            serviceConfig1.setInterface(DemoService.class);
            serviceConfig1.setRef(new DemoServiceImpl());
            serviceConfig1.setVersion(version1);

            ServiceConfig serviceConfig2 = new ServiceConfig();
            serviceConfig2.setInterface(DemoService.class);
            serviceConfig2.setRef(new DemoServiceImpl());
            serviceConfig2.setVersion(version2);

            ServiceConfig serviceConfig3 = new ServiceConfig();
            serviceConfig3.setInterface(DemoService.class);
            serviceConfig3.setRef(new DemoServiceImpl());
            serviceConfig3.setVersion(version3);

            providerBootstrap
                .application("provider-app")
                .registry(registryConfig)
                .protocol(new ProtocolConfig("dubbo", -1))
                .service(serviceConfig1)
                .newModule()
                .service(serviceConfig2)
                .endModule()
                .newModule()
                .service(serviceConfig3)
                .endModule();

            ApplicationModel applicationModel = providerBootstrap.getApplicationModel();
            List<ModuleModel> moduleModels = applicationModel.getModuleModels();
            Assertions.assertEquals(4, moduleModels.size());
            Assertions.assertSame(moduleModels.get(0), applicationModel.getInternalModule());
            Assertions.assertSame(moduleModels.get(1), applicationModel.getDefaultModule());
            Assertions.assertSame(applicationModel.getDefaultModule(), serviceConfig1.getScopeModel());
            Assertions.assertSame(moduleModels.get(2), serviceConfig2.getScopeModel());
            Assertions.assertSame(moduleModels.get(3), serviceConfig3.getScopeModel());
            Assertions.assertNotSame(applicationModel.getDefaultModule(), applicationModel.getInternalModule());

            providerBootstrap.start();

            Thread.sleep(100);

            // consumer app
            consumerBootstrap = DubboBootstrap.newInstance();
            consumerBootstrap.application("consumer-app")
                .registry(registryConfig)
                .reference(builder -> builder
                    .interfaceClass(DemoService.class)
                    .version(version1)
                    .injvm(false))
                .newModule()
                .reference(builder -> builder
                    .interfaceClass(DemoService.class)
                    .version(version2)
                    .injvm(false))
                .endModule();
            consumerBootstrap.start();

            DemoService referProxy1 = consumerBootstrap.getCache().get(DemoService.class.getName() + ":" + version1);
            Assertions.assertEquals("say:dubbo", referProxy1.sayName("dubbo"));

            DemoService referProxy2 = consumerBootstrap.getCache().get(DemoService.class.getName() + ":" + version2);
            Assertions.assertEquals("say:dubbo", referProxy2.sayName("dubbo"));

            Assertions.assertNotEquals(referProxy1, referProxy2);
        } finally {
            if (providerBootstrap != null) {
                providerBootstrap.destroy();
            }
            if (consumerBootstrap != null) {
                consumerBootstrap.destroy();
            }
        }

    }

    @Test
    public void testMultiModuleDeployAndReload() throws Exception {

        String version1 = "1.0";
        String version2 = "2.0";
        String version3 = "3.0";

        String serviceKey1 = DemoService.class.getName() + ":" + version1;
        String serviceKey2 = DemoService.class.getName() + ":" + version2;
        String serviceKey3 = DemoService.class.getName() + ":" + version3;

        DubboBootstrap providerBootstrap = null;
        DubboBootstrap consumerBootstrap = null;

        try {
            // provider app
            providerBootstrap = DubboBootstrap.newInstance();

            ServiceConfig serviceConfig1 = new ServiceConfig();
            serviceConfig1.setInterface(DemoService.class);
            serviceConfig1.setRef(new DemoServiceImpl());
            serviceConfig1.setVersion(version1);

            //provider module 1
            providerBootstrap
                .application("provider-app")
                .registry(registryConfig)
                .protocol(new ProtocolConfig("dubbo", -1))
                .service(builder -> builder
                    .interfaceClass(Greeting.class)
                    .ref(new GreetingLocal2()))
                .newModule()
                .service(serviceConfig1)
                .endModule();

            ApplicationModel applicationModel = providerBootstrap.getApplicationModel();
            List<ModuleModel> moduleModels = applicationModel.getModuleModels();
            Assertions.assertEquals(3, moduleModels.size());
            Assertions.assertSame(moduleModels.get(0), applicationModel.getInternalModule());
            Assertions.assertSame(moduleModels.get(1), applicationModel.getDefaultModule());
            Assertions.assertSame(moduleModels.get(2), serviceConfig1.getScopeModel());

            ModuleDeployer moduleDeployer1 = serviceConfig1.getScopeModel().getDeployer();
            moduleDeployer1.start().get();
            Assertions.assertTrue(moduleDeployer1.isStarted());
            ModuleDeployer internalModuleDeployer = applicationModel.getInternalModule().getDeployer();
            Assertions.assertTrue(internalModuleDeployer.isStarted());

            FrameworkServiceRepository frameworkServiceRepository = applicationModel.getFrameworkModel().getServiceRepository();
            Assertions.assertNotNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey1));
            Assertions.assertNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey2));
            Assertions.assertNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey3));

            // consumer module 1
            consumerBootstrap = DubboBootstrap.newInstance();
            consumerBootstrap.application("consumer-app")
                .registry(registryConfig)
                .reference(builder -> builder
                    .interfaceClass(DemoService.class)
                    .version(version1)
                    .injvm(false));
            consumerBootstrap.start();

            DemoService referProxy1 = consumerBootstrap.getCache().get(serviceKey1);
            String result1 = referProxy1.sayName("dubbo");
            Assertions.assertEquals("say:dubbo", result1);

            // destroy provider module 1
            serviceConfig1.getScopeModel().destroy();

            // provider module 2
            ServiceConfig serviceConfig2 = new ServiceConfig();
            serviceConfig2.setInterface(DemoService.class);
            serviceConfig2.setRef(new DemoServiceImpl());
            serviceConfig2.setVersion(version2);

            providerBootstrap.newModule()
                .service(serviceConfig2)
                .endModule();

            serviceConfig2.getScopeModel().getDeployer().start();
            Assertions.assertNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey1));
            Assertions.assertNotNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey2));
            Assertions.assertNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey3));

            // consumer module2
            ModuleModel consumerModule2 = consumerBootstrap.newModule()
                .reference(builder -> builder
                    .interfaceClass(DemoService.class)
                    .version(version2)
                    .injvm(false))
                .getModuleModel();

            ModuleDeployer moduleDeployer2 = consumerModule2.getDeployer();
            moduleDeployer2.start().get();

            DemoService referProxy2 = moduleDeployer2.getReferenceCache().get(serviceKey2);
            String result2 = referProxy2.sayName("dubbo2");
            Assertions.assertEquals("say:dubbo2", result2);

            // destroy provider module 2
            serviceConfig2.getScopeModel().destroy();

            // provider module 3
            ServiceConfig serviceConfig3 = new ServiceConfig();
            serviceConfig3.setInterface(DemoService.class);
            serviceConfig3.setRef(new DemoServiceImpl());
            serviceConfig3.setVersion(version3);

            providerBootstrap.newModule()
                .service(serviceConfig3)
                .endModule();

            serviceConfig3.getScopeModel().getDeployer().start().get();
            Assertions.assertNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey1));
            Assertions.assertNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey2));
            Assertions.assertNotNull(frameworkServiceRepository.lookupExportedServiceWithoutGroup(serviceKey3));

            // consumer module3
            ModuleModel consumerModule3 = consumerBootstrap.newModule()
                .reference(builder -> builder
                    .interfaceClass(DemoService.class)
                    .version(version3)
                    .injvm(false))
                .getModuleModel();

            consumerBootstrap.start();

            DemoService referProxy3 = consumerModule3.getDeployer().getReferenceCache().get(serviceKey3);
            String result3 = referProxy3.sayName("dubbo3");
            Assertions.assertEquals("say:dubbo3", result3);

        } finally {
            if (providerBootstrap != null) {
                providerBootstrap.destroy();
            }
            if (consumerBootstrap != null) {
                consumerBootstrap.destroy();
            }
        }
    }


    private DubboBootstrap configConsumerApp(DubboBootstrap dubboBootstrap) {
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setInjvm(false);

        if (!dubboBootstrap.getConfigManager().getApplication().isPresent()) {
            dubboBootstrap.application("consumer-app");
        }
        dubboBootstrap.registry(registryConfig)
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
        dubboBootstrap.registry(registryConfig)
            .protocol(protocol1)
            .service(serviceConfig);
        return dubboBootstrap;
    }

}
