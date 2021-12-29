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

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.mock.GreetingLocal2;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.test.check.DubboTestChecker;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_BOSS_POOL_NAME;

public class MultiInstanceTest {

    private static final Logger logger = LoggerFactory.getLogger(MultiInstanceTest.class);

    private static RegistryConfig registryConfig;

    private static DubboTestChecker testChecker;
    private static String testClassName;

    @BeforeAll
    public static void beforeAll() {
        FrameworkModel.destroyAll();
        registryConfig = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress1());

        // pre-check threads
        //precheckUnclosedThreads();
    }

    @AfterAll
    public static void afterAll() throws Exception {
        FrameworkModel.destroyAll();

        // check threads
        //checkUnclosedThreads();
    }

    private static Map<Thread, StackTraceElement[]> precheckUnclosedThreads() throws IOException {
        // create a special DubboTestChecker
        if (testChecker == null) {
            testChecker = new DubboTestChecker();
            testChecker.init(null);
            testClassName = MultiInstanceTest.class.getName();
        }
        return testChecker.checkUnclosedThreads(testClassName, 0);
    }

    private static void checkUnclosedThreads() {
        Map<Thread, StackTraceElement[]> unclosedThreadMap = testChecker.checkUnclosedThreads(testClassName, 3000);
        if (unclosedThreadMap.size() > 0) {
            String str = getStackTraceString(unclosedThreadMap);
            Assertions.fail("Found unclosed threads: " + unclosedThreadMap.size()+"\n" + str);
        }
    }

    private static String getStackTraceString(Map<Thread, StackTraceElement[]> unclosedThreadMap) {
        StringBuilder sb = new StringBuilder();
        for (Thread thread : unclosedThreadMap.keySet()) {
            sb.append(DubboTestChecker.getFullStacktrace(thread, unclosedThreadMap.get(thread)));
            sb.append("\n");
        }
        return sb.toString();
    }

    @BeforeEach
    public void setup() {

    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
        DubboBootstrap.reset();
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

        //SysProps.setProperty(METADATA_PUBLISH_DELAY_KEY, "100");
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

            //Thread.sleep(200);

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
    public void testMultiProviderApplicationsStopOneByOne() {

        String version1 = "1.0";
        String version2 = "2.0";

        DubboBootstrap providerBootstrap1 = null;
        DubboBootstrap providerBootstrap2 = null;

        try {

            // save threads before provider app 1
            Map<Thread, StackTraceElement[]> stackTraces0 = Thread.getAllStackTraces();

            // start provider app 1
            ServiceConfig serviceConfig1 = new ServiceConfig();
            serviceConfig1.setInterface(DemoService.class);
            serviceConfig1.setRef(new DemoServiceImpl());
            serviceConfig1.setVersion(version1);

            ProtocolConfig protocolConfig1 = new ProtocolConfig("dubbo", NetUtils.getAvailablePort());

            providerBootstrap1 = DubboBootstrap.getInstance();
            providerBootstrap1.application("provider1")
                .registry(registryConfig)
                .service(serviceConfig1)
                .protocol(protocolConfig1)
                .start();

            // save threads of provider app 1
            Map<Thread, StackTraceElement[]> lastAllThreadStackTraces = Thread.getAllStackTraces();
            Map<Thread, StackTraceElement[]> stackTraces1 = findNewThreads(lastAllThreadStackTraces, stackTraces0);
            Assertions.assertTrue(stackTraces1.size() > 0, "Get threads of provider app 1 failed");

            // start zk server 2
            RegistryConfig registryConfig2 = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress2());

            // start provider app 2 use a difference zk server 2
            ServiceConfig serviceConfig2 = new ServiceConfig();
            serviceConfig2.setInterface(DemoService.class);
            serviceConfig2.setRef(new DemoServiceImpl());
            serviceConfig2.setVersion(version2);

            ProtocolConfig protocolConfig2 = new ProtocolConfig("dubbo", NetUtils.getAvailablePort());

            providerBootstrap2 = DubboBootstrap.newInstance();
            providerBootstrap2.application("provider2")
                .registry(registryConfig2)
                .service(serviceConfig2)
                .protocol(protocolConfig2)
                .start();

            // save threads of provider app 2
            Map<Thread, StackTraceElement[]> stackTraces2 = findNewThreads(Thread.getAllStackTraces(), stackTraces0);
            Assertions.assertTrue(stackTraces2.size() > 0, "Get threads of provider app 2 failed");

            // stop provider app 1 and check threads
            providerBootstrap1.stop();

            // TODO Remove ignore thread prefix of NettyServerBoss if supporting close protocol server only used by one application
            // see org.apache.dubbo.config.deploy.DefaultApplicationDeployer.postDestroy
            // NettyServer will close when all applications are shutdown, but not close if any application of the framework is alive, just ignore it currently
            checkUnclosedThreadsOfApp(stackTraces1, "Found unclosed threads of app 1: ", new String[]{EVENT_LOOP_BOSS_POOL_NAME, "Dubbo-global-shared-handler"});


            // stop provider app 2 and check threads
            providerBootstrap2.stop();
            // shutdown register center after dubbo application to avoid unregister services blocking
            checkUnclosedThreadsOfApp(stackTraces2, "Found unclosed threads of app 2: ", null);

        } finally {
            if (providerBootstrap1 != null) {
                providerBootstrap1.stop();
            }
            if (providerBootstrap2 != null) {
                providerBootstrap2.stop();
            }
        }
    }

    private Map<Thread, StackTraceElement[]> findNewThreads(Map<Thread, StackTraceElement[]> newAllThreadMap, Map<Thread, StackTraceElement[]> prevThreadMap) {
        Map<Thread, StackTraceElement[]> deltaThreadMap = new HashMap<>(newAllThreadMap);
        deltaThreadMap.keySet().removeAll(prevThreadMap.keySet());
        // expect deltaThreadMap not contains any elements of prevThreadMap
        Assertions.assertFalse(deltaThreadMap.keySet().stream().filter(thread -> prevThreadMap.containsKey(thread)).findAny().isPresent());
        return deltaThreadMap;
    }

    private void checkUnclosedThreadsOfApp(Map<Thread, StackTraceElement[]> stackTraces1, String msg, String[] ignoredThreadPrefixes) {
        int waitTimeMs = 5000;
        System.out.println("Wait "+waitTimeMs+"ms to check threads of app ...");
        try {
            Thread.sleep(waitTimeMs);
        } catch (InterruptedException e) {
        }
        HashMap<Thread, StackTraceElement[]> unclosedThreadMap1 = new HashMap<>(stackTraces1);
        unclosedThreadMap1.keySet().removeIf(thread -> !thread.isAlive());
        if (ignoredThreadPrefixes!= null && ignoredThreadPrefixes.length > 0) {
            unclosedThreadMap1.keySet().removeIf(thread -> isIgnoredThread(thread.getName(), ignoredThreadPrefixes));
        }
        if (unclosedThreadMap1.size() > 0) {
            String str = getStackTraceString(unclosedThreadMap1);
            Assertions.fail(msg + unclosedThreadMap1.size()+"\n" + str);
        }
    }

    private boolean isIgnoredThread(String name, String[] ignoredThreadPrefixes) {
        if (ignoredThreadPrefixes!= null && ignoredThreadPrefixes.length > 0) {
            for (String prefix : ignoredThreadPrefixes) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
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

            // start provider module 2 and wait
            serviceConfig2.getScopeModel().getDeployer().start().get();
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

    @Test
    public void testBothStartByModuleAndByApplication() throws Exception {
        String version1 = "1.0";
        String version2 = "2.0";
        String version3 = "3.0";

        String serviceKey1 = DemoService.class.getName() + ":" + version1;
        String serviceKey2 = DemoService.class.getName() + ":" + version2;
        String serviceKey3 = DemoService.class.getName() + ":" + version3;

        // provider app
        DubboBootstrap providerBootstrap = null;
        try {
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

            // 1. start module1 and wait
            ModuleDeployer moduleDeployer1 = serviceConfig1.getScopeModel().getDeployer();
            moduleDeployer1.start().get();
            Assertions.assertEquals(DeployState.STARTED, moduleDeployer1.getState());

            ApplicationModel applicationModel = providerBootstrap.getApplicationModel();
            ApplicationDeployer applicationDeployer = applicationModel.getDeployer();
            Assertions.assertEquals(DeployState.STARTING, applicationDeployer.getState());
            ModuleModel defaultModule = applicationModel.getDefaultModule();
            Assertions.assertEquals(DeployState.PENDING, defaultModule.getDeployer().getState());

            // 2. start application after module1 is started
            providerBootstrap.start();
            Assertions.assertEquals(DeployState.STARTED, applicationDeployer.getState());
            Assertions.assertEquals(DeployState.STARTED, defaultModule.getDeployer().getState());
            
            // 3. add module2 and re-start application
            ServiceConfig serviceConfig2 = new ServiceConfig();
            serviceConfig2.setInterface(DemoService.class);
            serviceConfig2.setRef(new DemoServiceImpl());
            serviceConfig2.setVersion(version2);
            ModuleModel moduleModel2 = providerBootstrap.newModule()
                .service(serviceConfig2)
                .getModuleModel();
            providerBootstrap.start();
            Assertions.assertEquals(DeployState.STARTED, applicationDeployer.getState());
            Assertions.assertEquals(DeployState.STARTED, moduleModel2.getDeployer().getState());
            
            // 4. add module3 and start module3
            ServiceConfig serviceConfig3 = new ServiceConfig();
            serviceConfig3.setInterface(DemoService.class);
            serviceConfig3.setRef(new DemoServiceImpl());
            serviceConfig3.setVersion(version3);
            ModuleModel moduleModel3 = providerBootstrap.newModule()
                .service(serviceConfig3)
                .getModuleModel();
            moduleModel3.getDeployer().start().get();
            Assertions.assertEquals(DeployState.STARTED, applicationDeployer.getState());
            Assertions.assertEquals(DeployState.STARTED, moduleModel3.getDeployer().getState());

        } finally {
            if (providerBootstrap != null) {
                providerBootstrap.stop();
            }
        }
    }


    @Test
    public void testBothStartModuleAndApplicationNoWait() throws Exception {
        String version1 = "1.0";
        String version2 = "2.0";
        String version3 = "3.0";

        String serviceKey1 = DemoService.class.getName() + ":" + version1;
        String serviceKey2 = DemoService.class.getName() + ":" + version2;
        String serviceKey3 = DemoService.class.getName() + ":" + version3;

        // provider app
        DubboBootstrap providerBootstrap = null;
        try {
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

            // 1. start module1 but no wait
            ModuleDeployer moduleDeployer1 = serviceConfig1.getScopeModel().getDeployer();
            moduleDeployer1.start();
            Assertions.assertTrue(moduleDeployer1.isRunning());

            ApplicationDeployer applicationDeployer = applicationModel.getDeployer();
            Assertions.assertEquals(DeployState.STARTING, applicationDeployer.getState());
            ModuleModel defaultModule = applicationModel.getDefaultModule();
            Assertions.assertEquals(DeployState.PENDING, defaultModule.getDeployer().getState());

            // 2. start application after module1 is starting
            providerBootstrap.start();
            Assertions.assertEquals(DeployState.STARTED, applicationDeployer.getState());
            Assertions.assertEquals(DeployState.STARTED, moduleDeployer1.getState());
            Assertions.assertEquals(DeployState.STARTED, defaultModule.getDeployer().getState());

        } finally {
            if (providerBootstrap != null) {
                providerBootstrap.stop();
            }
        }
    }

    @Test
    public void testOldApiDeploy() throws Exception {

        try {
            // provider app
            ApplicationModel providerApplicationModel = ApplicationModel.defaultModel();
            ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setScopeModel(providerApplicationModel.getDefaultModule());
            serviceConfig.setRef(new DemoServiceImpl());
            serviceConfig.setInterface(DemoService.class);
            serviceConfig.setApplication(new ApplicationConfig("provider-app"));
            serviceConfig.setRegistry(new RegistryConfig(registryConfig.getAddress()));
            // add service
            //serviceConfig.getScopeModel().getConfigManager().addService(serviceConfig);

            // detect deploy events
            DeployEventHandler serviceDeployEventHandler = new DeployEventHandler(serviceConfig.getScopeModel());
            serviceConfig.getScopeModel().getDeployer().addDeployListener(serviceDeployEventHandler);
            // before starting
            Map<DeployState, Long> serviceDeployEventMap = serviceDeployEventHandler.deployEventMap;
            Assertions.assertFalse(serviceDeployEventMap.containsKey(DeployState.STARTING));
            Assertions.assertFalse(serviceDeployEventMap.containsKey(DeployState.STARTED));

            // export service and start module
            serviceConfig.export();
            // expect internal module is started
            Assertions.assertTrue(providerApplicationModel.getInternalModule().getDeployer().isStarted());
            // expect service module is starting
            Assertions.assertTrue(serviceDeployEventMap.containsKey(DeployState.STARTING));
            // wait for service module started
            serviceConfig.getScopeModel().getDeployer().getStartFuture().get();
            Assertions.assertTrue(serviceDeployEventMap.containsKey(DeployState.STARTED));


            // consumer app
            ApplicationModel consumerApplicationModel = new ApplicationModel(FrameworkModel.defaultModel());
            ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
            referenceConfig.setScopeModel(consumerApplicationModel.getDefaultModule());
            referenceConfig.setApplication(new ApplicationConfig("consumer-app"));
            referenceConfig.setInterface(DemoService.class);
            referenceConfig.setRegistry(new RegistryConfig(registryConfig.getAddress()));
            referenceConfig.setScope("remote");

            // detect deploy events
            DeployEventHandler referDeployEventHandler = new DeployEventHandler(referenceConfig.getScopeModel());
            referenceConfig.getScopeModel().getDeployer().addDeployListener(referDeployEventHandler);

            // before starting
            Map<DeployState, Long> deployEventMap = referDeployEventHandler.deployEventMap;
            Assertions.assertFalse(deployEventMap.containsKey(DeployState.STARTING));
            Assertions.assertFalse(deployEventMap.containsKey(DeployState.STARTED));

            // get ref proxy and start module
            DemoService demoService = referenceConfig.get();
            // expect internal module is started
            Assertions.assertTrue(consumerApplicationModel.getInternalModule().getDeployer().isStarted());
            Assertions.assertTrue(deployEventMap.containsKey(DeployState.STARTING));
            // wait for reference module started
            referenceConfig.getScopeModel().getDeployer().getStartFuture().get();
            Assertions.assertTrue(deployEventMap.containsKey(DeployState.STARTED));

            // stop consumer app
            consumerApplicationModel.destroy();
            Assertions.assertTrue(deployEventMap.containsKey(DeployState.STOPPING));
            Assertions.assertTrue(deployEventMap.containsKey(DeployState.STOPPED));

            // stop provider app
            providerApplicationModel.destroy();
            Assertions.assertTrue(serviceDeployEventMap.containsKey(DeployState.STOPPING));
            Assertions.assertTrue(serviceDeployEventMap.containsKey(DeployState.STOPPED));

        } finally {
            FrameworkModel.destroyAll();
        }
    }

    @Test
    public void testAsyncExportAndReferServices() throws ExecutionException, InterruptedException {
        DubboBootstrap providerBootstrap = DubboBootstrap.newInstance();
        DubboBootstrap consumerBootstrap = DubboBootstrap.newInstance();
        try {

            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setInterface(Greeting.class);
            serviceConfig.setRef(new GreetingLocal2());
            serviceConfig.setExportAsync(true);

            ReferenceConfig<Greeting> referenceConfig = new ReferenceConfig<>();
            referenceConfig.setInterface(Greeting.class);
            referenceConfig.setInjvm(false);
            referenceConfig.setReferAsync(true);
            referenceConfig.setCheck(false);

            // provider app
            Future providerFuture = providerBootstrap
                .application("provider-app")
                .registry(registryConfig)
                .protocol(new ProtocolConfig("dubbo", -1))
                .service(serviceConfig)
                .asyncStart();
            logger.warn("provider app has start async");
            // it might be started if running on fast machine.
            // Assertions.assertFalse(serviceConfig.getScopeModel().getDeployer().isStarted(), "Async export seems something wrong");

            // consumer app
            Future consumerFuture = consumerBootstrap
                .application("consumer-app")
                .registry(registryConfig)
                .reference(referenceConfig)
                .asyncStart();
            logger.warn("consumer app has start async");
            // it might be started if running on fast machine.
            // Assertions.assertFalse(referenceConfig.getScopeModel().getDeployer().isStarted(), "Async refer seems something wrong");

            // wait for provider app startup
            providerFuture.get();
            logger.warn("provider app is startup");
            Assertions.assertEquals(true, serviceConfig.isExported());
            ServiceDescriptor serviceDescriptor = serviceConfig.getScopeModel().getServiceRepository().lookupService(Greeting.class.getName());
            Assertions.assertNotNull(serviceDescriptor);

            // wait for consumer app startup
            consumerFuture.get();
            logger.warn("consumer app is startup");
            Object target = referenceConfig.getServiceMetadata().getTarget();
            Assertions.assertNotNull(target);
            // wait for invokers notified from registry
            MigrationInvoker migrationInvoker = (MigrationInvoker) referenceConfig.getInvoker(); 
            for (int i = 0; i < 10; i++) {
                if (((List<Invoker>) migrationInvoker.getDirectory().getAllInvokers())
                        .stream().anyMatch(invoker -> invoker.getInterface() == Greeting.class)) {
                    break;
                }
                Thread.sleep(100);
            }
            Greeting greetingService = (Greeting) target;
            String result = greetingService.hello();
            Assertions.assertEquals("local", result);
        } finally {
            providerBootstrap.stop();
            consumerBootstrap.stop();
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

    private static class DeployEventHandler implements DeployListener<ModuleModel> {

        Map<DeployState, Long> deployEventMap = new LinkedHashMap<>();

        ModuleModel moduleModel;

        public DeployEventHandler(ModuleModel moduleModel) {
            this.moduleModel = moduleModel;
        }

        @Override
        public void onStarting(ModuleModel scopeModel) {
            Assertions.assertEquals(moduleModel, scopeModel);
            deployEventMap.put(DeployState.STARTING, System.currentTimeMillis());
        }

        @Override
        public void onStarted(ModuleModel scopeModel) {
            Assertions.assertEquals(moduleModel, scopeModel);
            deployEventMap.put(DeployState.STARTED, System.currentTimeMillis());
        }

        @Override
        public void onStopping(ModuleModel scopeModel) {
            Assertions.assertEquals(moduleModel, scopeModel);
            deployEventMap.put(DeployState.STOPPING, System.currentTimeMillis());
        }

        @Override
        public void onStopped(ModuleModel scopeModel) {
            Assertions.assertEquals(moduleModel, scopeModel);
            deployEventMap.put(DeployState.STOPPED, System.currentTimeMillis());
        }

        @Override
        public void onFailure(ModuleModel scopeModel, Throwable cause) {
            Assertions.assertEquals(moduleModel, scopeModel);
            deployEventMap.put(DeployState.FAILED, System.currentTimeMillis());
        }
    }
}
