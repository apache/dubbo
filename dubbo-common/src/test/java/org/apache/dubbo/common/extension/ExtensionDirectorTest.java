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
package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.extension.director.FooAppService;
import org.apache.dubbo.common.extension.director.FooFrameworkService;
import org.apache.dubbo.common.extension.director.FooModuleService;
import org.apache.dubbo.common.extension.director.impl.TestAppService;
import org.apache.dubbo.common.extension.director.impl.TestFrameworkService;
import org.apache.dubbo.common.extension.director.impl.TestModuleService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtensionDirectorTest {

    String testFwSrvName = "testFwSrv";
    String testAppSrvName = "testAppSrv";
    String testMdSrvName = "testMdSrv";

    @Test
    void testInheritanceAndScope() {

        // Expecting:
        // 1. SPI extension only be created in ExtensionDirector which matched scope
        // 2. Child ExtensionDirector can get extension instance from parent
        // 3. Parent ExtensionDirector can't get extension instance from child

        ExtensionDirector fwExtensionDirector = FrameworkModel.defaultModel().getExtensionDirector();
        ExtensionDirector appExtensionDirector =
                new ExtensionDirector(fwExtensionDirector, ExtensionScope.APPLICATION, ApplicationModel.defaultModel());
        ExtensionDirector moduleExtensionDirector = new ExtensionDirector(
                appExtensionDirector,
                ExtensionScope.MODULE,
                ApplicationModel.defaultModel().getDefaultModule());

        // test module extension loader
        FooFrameworkService testFwSrvFromModule =
                moduleExtensionDirector.getExtension(FooFrameworkService.class, testFwSrvName);
        FooAppService testAppSrvFromModule = moduleExtensionDirector.getExtension(FooAppService.class, testAppSrvName);
        FooModuleService testMdSrvFromModule =
                moduleExtensionDirector.getExtension(FooModuleService.class, testMdSrvName);

        Assertions.assertNotNull(testFwSrvFromModule);
        Assertions.assertNotNull(testAppSrvFromModule);
        Assertions.assertNotNull(testMdSrvFromModule);

        // test app extension loader
        FooFrameworkService testFwSrvFromApp =
                appExtensionDirector.getExtension(FooFrameworkService.class, testFwSrvName);
        FooAppService testAppSrvFromApp = appExtensionDirector.getExtension(FooAppService.class, testAppSrvName);
        FooModuleService testMdSrvFromApp = appExtensionDirector.getExtension(FooModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromApp, testFwSrvFromModule);
        Assertions.assertSame(testAppSrvFromApp, testAppSrvFromModule);
        Assertions.assertNull(testMdSrvFromApp);

        // test framework extension loader
        FooFrameworkService testFwSrvFromFw =
                fwExtensionDirector.getExtension(FooFrameworkService.class, testFwSrvName);
        FooAppService testAppSrvFromFw = fwExtensionDirector.getExtension(FooAppService.class, testAppSrvName);
        FooModuleService testMdSrvFromFw = fwExtensionDirector.getExtension(FooModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromFw, testFwSrvFromApp);
        Assertions.assertNull(testAppSrvFromFw);
        Assertions.assertNull(testMdSrvFromFw);
    }

    @Test
    void testPostProcessor() {}

    @Test
    void testModelAware() {
        // Expecting:
        // 1. Module scope SPI can be injected ModuleModel, ApplicationModel, FrameworkModel
        // 2. Application scope SPI can be injected ApplicationModel, FrameworkModel, but not ModuleModel
        // 3. Framework scope SPI can be injected FrameworkModel, but not ModuleModel, ApplicationModel

        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        ExtensionDirector moduleExtensionDirector = moduleModel.getExtensionDirector();
        ExtensionDirector appExtensionDirector = applicationModel.getExtensionDirector();
        ExtensionDirector fwExtensionDirector = frameworkModel.getExtensionDirector();

        // check extension director inheritance
        Assertions.assertSame(appExtensionDirector, moduleExtensionDirector.getParent());
        Assertions.assertSame(fwExtensionDirector, appExtensionDirector.getParent());
        Assertions.assertSame(null, fwExtensionDirector.getParent());

        // check module extension aware
        TestFrameworkService testFwSrvFromModule =
                (TestFrameworkService) moduleExtensionDirector.getExtension(FooFrameworkService.class, testFwSrvName);
        TestAppService testAppSrvFromModule =
                (TestAppService) moduleExtensionDirector.getExtension(FooAppService.class, testAppSrvName);
        TestModuleService testMdSrvFromModule =
                (TestModuleService) moduleExtensionDirector.getExtension(FooModuleService.class, testMdSrvName);

        Assertions.assertSame(frameworkModel, testFwSrvFromModule.getFrameworkModel());
        Assertions.assertSame(null, testFwSrvFromModule.getApplicationModel());
        Assertions.assertSame(null, testFwSrvFromModule.getModuleModel());

        Assertions.assertSame(frameworkModel, testAppSrvFromModule.getFrameworkModel());
        Assertions.assertSame(applicationModel, testAppSrvFromModule.getApplicationModel());
        Assertions.assertSame(null, testAppSrvFromModule.getModuleModel());

        Assertions.assertSame(frameworkModel, testMdSrvFromModule.getFrameworkModel());
        Assertions.assertSame(applicationModel, testMdSrvFromModule.getApplicationModel());
        Assertions.assertSame(moduleModel, testMdSrvFromModule.getModuleModel());

        // check app extension aware
        TestFrameworkService testFwSrvFromApp =
                (TestFrameworkService) appExtensionDirector.getExtension(FooFrameworkService.class, testFwSrvName);
        TestAppService testAppSrvFromApp =
                (TestAppService) appExtensionDirector.getExtension(FooAppService.class, testAppSrvName);
        TestModuleService testMdSrvFromApp =
                (TestModuleService) appExtensionDirector.getExtension(FooModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromApp, testFwSrvFromModule);
        Assertions.assertSame(testAppSrvFromApp, testAppSrvFromModule);
        Assertions.assertNull(testMdSrvFromApp);

        // check framework extension aware
        FooFrameworkService testFwSrvFromFw =
                fwExtensionDirector.getExtension(FooFrameworkService.class, testFwSrvName);
        FooAppService testAppSrvFromFw = fwExtensionDirector.getExtension(FooAppService.class, testAppSrvName);
        FooModuleService testMdSrvFromFw = fwExtensionDirector.getExtension(FooModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromFw, testFwSrvFromApp);
        Assertions.assertNull(testAppSrvFromFw);
        Assertions.assertNull(testMdSrvFromFw);
    }

    @Test
    void testModelDataIsolation() {
        // Model Tree
        // ├─frameworkModel1
        // │  ├─applicationModel11
        // │  │  ├─moduleModel111
        // │  │  └─moduleModel112
        // │  └─applicationModel12
        // │     └─moduleModel121
        // └─frameworkModel2
        //   └─applicationModel21
        //      └─moduleModel211

        FrameworkModel frameworkModel1 = new FrameworkModel();
        ApplicationModel applicationModel11 = frameworkModel1.newApplication();
        ModuleModel moduleModel111 = applicationModel11.newModule();
        ModuleModel moduleModel112 = applicationModel11.newModule();

        ApplicationModel applicationModel12 = frameworkModel1.newApplication();
        ModuleModel moduleModel121 = applicationModel12.newModule();

        FrameworkModel frameworkModel2 = new FrameworkModel();
        ApplicationModel applicationModel21 = frameworkModel2.newApplication();
        ModuleModel moduleModel211 = applicationModel21.newModule();

        // test model references
        Collection<ApplicationModel> applicationsOfFw1 = frameworkModel1.getApplicationModels();
        Assertions.assertEquals(2, applicationsOfFw1.size());
        Assertions.assertTrue(applicationsOfFw1.contains(applicationModel11));
        Assertions.assertTrue(applicationsOfFw1.contains(applicationModel12));
        Assertions.assertFalse(applicationsOfFw1.contains(applicationModel21));

        Collection<ModuleModel> modulesOfApp11 = applicationModel11.getModuleModels();
        Assertions.assertTrue(modulesOfApp11.contains(moduleModel111));
        Assertions.assertTrue(modulesOfApp11.contains(moduleModel112));

        // test isolation of FrameworkModel
        FooFrameworkService frameworkService1 =
                frameworkModel1.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        FooFrameworkService frameworkService2 =
                frameworkModel2.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        Assertions.assertNotSame(frameworkService1, frameworkService2);

        // test isolation of ApplicationModel
        // applicationModel11 and applicationModel12 are shared frameworkModel1
        FooFrameworkService frameworkService11 =
                applicationModel11.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        FooFrameworkService frameworkService12 =
                applicationModel12.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        Assertions.assertSame(frameworkService1, frameworkService11);
        Assertions.assertSame(frameworkService1, frameworkService12);

        // applicationModel11 and applicationModel12 are isolated in application scope
        FooAppService applicationService11 =
                applicationModel11.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        FooAppService applicationService12 =
                applicationModel12.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        Assertions.assertNotSame(applicationService11, applicationService12);

        // applicationModel11 and applicationModel21 are isolated in both framework and application scope
        FooFrameworkService frameworkService21 =
                applicationModel21.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        FooAppService applicationService21 =
                applicationModel21.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        Assertions.assertNotSame(frameworkService11, frameworkService21);
        Assertions.assertNotSame(applicationService11, applicationService21);

        // test isolation of ModuleModel
        FooModuleService moduleService111 =
                moduleModel111.getExtensionDirector().getExtension(FooModuleService.class, testMdSrvName);
        FooModuleService moduleService112 =
                moduleModel112.getExtensionDirector().getExtension(FooModuleService.class, testMdSrvName);

        // moduleModel111 and moduleModel112 are isolated in module scope
        Assertions.assertNotSame(moduleService111, moduleService112);

        // moduleModel111 and moduleModel112 are shared applicationModel11
        FooAppService applicationService111 =
                moduleModel111.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        FooAppService applicationService112 =
                moduleModel112.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        Assertions.assertSame(applicationService111, applicationService112);

        // moduleModel111 and moduleModel121 are isolated in application scope, but shared frameworkModel1
        FooAppService applicationService121 =
                moduleModel121.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        Assertions.assertNotSame(applicationService111, applicationService121);

        FooFrameworkService frameworkService111 =
                moduleModel111.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        FooFrameworkService frameworkService121 =
                moduleModel121.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        Assertions.assertSame(frameworkService111, frameworkService121);

        // moduleModel111 and moduleModel211 are isolated in both framework and application scope
        FooModuleService moduleService211 =
                moduleModel211.getExtensionDirector().getExtension(FooModuleService.class, testMdSrvName);
        FooAppService applicationService211 =
                moduleModel211.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        FooFrameworkService frameworkService211 =
                moduleModel211.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        Assertions.assertNotSame(moduleService111, moduleService211);
        Assertions.assertNotSame(applicationService111, applicationService211);
        Assertions.assertNotSame(frameworkService111, frameworkService211);
    }

    @Test
    void testInjection() {

        // Expect:
        // 1. Framework scope extension can be injected to extensions of Framework/Application/Module scope
        // 2. Application scope extension can be injected to extensions of Application/Module scope, but not Framework
        // scope
        // 3. Module scope extension can be injected to extensions of Module scope, but not Framework/Application scope

        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        // check module service
        TestModuleService moduleService = (TestModuleService)
                moduleModel.getExtensionDirector().getExtension(FooModuleService.class, testMdSrvName);
        Assertions.assertNotNull(moduleService.getFrameworkService());
        Assertions.assertNotNull(moduleService.getFrameworkProvider());
        Assertions.assertNotNull(moduleService.getAppService());
        Assertions.assertNotNull(moduleService.getAppProvider());
        Assertions.assertNotNull(moduleService.getModuleProvider());

        // check app service
        TestAppService appService = (TestAppService)
                applicationModel.getExtensionDirector().getExtension(FooAppService.class, testAppSrvName);
        Assertions.assertNotNull(appService.getFrameworkService());
        Assertions.assertNotNull(appService.getFrameworkProvider());
        Assertions.assertNotNull(appService.getAppProvider());
        Assertions.assertNull(appService.getModuleProvider());

        // check framework service
        TestFrameworkService frameworkService = (TestFrameworkService)
                frameworkModel.getExtensionDirector().getExtension(FooFrameworkService.class, testFwSrvName);
        Assertions.assertNotNull(frameworkService.getFrameworkProvider());
        Assertions.assertNull(frameworkService.getAppProvider());
        Assertions.assertNull(frameworkService.getModuleProvider());

        Assertions.assertFalse(moduleService.isDestroyed());
        Assertions.assertFalse(appService.isDestroyed());
        Assertions.assertFalse(frameworkService.isDestroyed());

        // destroy
        frameworkModel.destroy();
        Assertions.assertTrue(moduleService.isDestroyed());
        Assertions.assertTrue(appService.isDestroyed());
        Assertions.assertTrue(frameworkService.isDestroyed());
    }
}
