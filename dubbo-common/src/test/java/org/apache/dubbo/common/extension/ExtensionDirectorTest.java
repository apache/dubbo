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

import org.apache.dubbo.common.extension.ExtensionDirector;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.director.ApplicationService;
import org.apache.dubbo.common.extension.director.FrameworkService;
import org.apache.dubbo.common.extension.director.ModuleService;
import org.apache.dubbo.common.extension.director.impl.TestApplicationService;
import org.apache.dubbo.common.extension.director.impl.TestFrameworkService;
import org.apache.dubbo.common.extension.director.impl.TestModuleService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExtensionDirectorTest {

    String testFwSrvName = "testFwSrv";
    String testAppSrvName = "testAppSrv";
    String testMdSrvName = "testMdSrv";

    @Test
    public void testInheritanceAndScope() {

        // expecting:
        // 1. SPI extension only be created in ExtensionDirector which matched scope
        // 2. Child ExtensionDirector can get extension instance from parent
        // 3. Parent ExtensionDirector can't get extension instance from child

        ExtensionDirector fwExtensionDirector = new ExtensionDirector(null, ExtensionScope.FRAMEWORK);
        ExtensionDirector appExtensionDirector = new ExtensionDirector(fwExtensionDirector, ExtensionScope.APPLICATION);
        ExtensionDirector moduleExtensionDirector = new ExtensionDirector(appExtensionDirector, ExtensionScope.MODULE);

        // test module extension loader
        FrameworkService testFwSrvFromModule = moduleExtensionDirector.getExtension(FrameworkService.class, testFwSrvName);
        ApplicationService testAppSrvFromModule = moduleExtensionDirector.getExtension(ApplicationService.class, testAppSrvName);
        ModuleService testMdSrvFromModule = moduleExtensionDirector.getExtension(ModuleService.class, testMdSrvName);

        Assertions.assertNotNull(testFwSrvFromModule);
        Assertions.assertNotNull(testAppSrvFromModule);
        Assertions.assertNotNull(testMdSrvFromModule);

        // test app extension loader
        FrameworkService testFwSrvFromApp = appExtensionDirector.getExtension(FrameworkService.class, testFwSrvName);
        ApplicationService testAppSrvFromApp = appExtensionDirector.getExtension(ApplicationService.class, testAppSrvName);
        ModuleService testMdSrvFromApp = appExtensionDirector.getExtension(ModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromApp, testFwSrvFromModule);
        Assertions.assertSame(testAppSrvFromApp, testAppSrvFromModule);
        Assertions.assertNull(testMdSrvFromApp);

        // test framework extension loader
        FrameworkService testFwSrvFromFw = fwExtensionDirector.getExtension(FrameworkService.class, testFwSrvName);
        ApplicationService testAppSrvFromFw = fwExtensionDirector.getExtension(ApplicationService.class, testAppSrvName);
        ModuleService testMdSrvFromFw = fwExtensionDirector.getExtension(ModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromFw, testFwSrvFromApp);
        Assertions.assertNull(testAppSrvFromFw);
        Assertions.assertNull(testMdSrvFromFw);
    }

    @Test
    public void testPostProcessor() {

    }

    @Test
    public void testModelAware() {
        // Expecting:
        // 1. Module scope SPI can be injected ModuleModel, ApplicationModel, FrameworkModel
        // 2. Application scope SPI can be injected ApplicationModel, FrameworkModel, but not ModuleModel
        // 3. Framework scope SPI can be injected FrameworkModel, but not ModuleModel, ApplicationModel

        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);
        ModuleModel moduleModel = new ModuleModel(applicationModel);

        ExtensionDirector moduleExtensionDirector = moduleModel.getExtensionDirector();
        ExtensionDirector appExtensionDirector = applicationModel.getExtensionDirector();
        ExtensionDirector fwExtensionDirector = frameworkModel.getExtensionDirector();

        // check extension director inheritance
        Assertions.assertSame(appExtensionDirector, moduleExtensionDirector.getParent());
        Assertions.assertSame(fwExtensionDirector, appExtensionDirector.getParent());
        Assertions.assertSame(null, fwExtensionDirector.getParent());

        // check module extension aware
        TestFrameworkService testFwSrvFromModule = (TestFrameworkService) moduleExtensionDirector.getExtension(FrameworkService.class, testFwSrvName);
        TestApplicationService testAppSrvFromModule = (TestApplicationService) moduleExtensionDirector.getExtension(ApplicationService.class, testAppSrvName);
        TestModuleService testMdSrvFromModule = (TestModuleService) moduleExtensionDirector.getExtension(ModuleService.class, testMdSrvName);

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
        TestFrameworkService testFwSrvFromApp = (TestFrameworkService) appExtensionDirector.getExtension(FrameworkService.class, testFwSrvName);
        TestApplicationService testAppSrvFromApp = (TestApplicationService) appExtensionDirector.getExtension(ApplicationService.class, testAppSrvName);
        TestModuleService testMdSrvFromApp = (TestModuleService) appExtensionDirector.getExtension(ModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromApp, testFwSrvFromModule);
        Assertions.assertSame(testAppSrvFromApp, testAppSrvFromModule);
        Assertions.assertNull(testMdSrvFromApp);

        // check framework extension aware
        FrameworkService testFwSrvFromFw = fwExtensionDirector.getExtension(FrameworkService.class, testFwSrvName);
        ApplicationService testAppSrvFromFw = fwExtensionDirector.getExtension(ApplicationService.class, testAppSrvName);
        ModuleService testMdSrvFromFw = fwExtensionDirector.getExtension(ModuleService.class, testMdSrvName);

        Assertions.assertSame(testFwSrvFromFw, testFwSrvFromApp);
        Assertions.assertNull(testAppSrvFromFw);
        Assertions.assertNull(testMdSrvFromFw);

    }
}
