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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.config.ConfigurationCache;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.support.MockScopeModelDestroyListener;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link ApplicationModel}
 */
public class ApplicationModelTest {

    @Test
    public void testInitialize() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);

        Assertions.assertEquals(applicationModel.getParent(), frameworkModel);
        Assertions.assertEquals(applicationModel.getScope(), ExtensionScope.APPLICATION);
        Assertions.assertEquals(applicationModel.getFrameworkModel(), frameworkModel);
        Assertions.assertFalse(applicationModel.isInternal());
        Assertions.assertTrue(frameworkModel.getApplicationModels().contains(applicationModel));
        Assertions.assertNotNull(applicationModel.getInternalId());

        Assertions.assertNotNull(applicationModel.getExtensionDirector());
        Assertions.assertNotNull(applicationModel.getBeanFactory());
        Assertions.assertTrue(applicationModel.getClassLoaders().contains(ScopeModel.class.getClassLoader()));

        Assertions.assertNotNull(applicationModel.getInternalModule());
        Assertions.assertNotNull(applicationModel.getApplicationServiceRepository());

        ScopeBeanFactory applicationModelBeanFactory = applicationModel.getBeanFactory();
        Assertions.assertNotNull(applicationModelBeanFactory.getBean(ShutdownHookCallbacks.class));
        Assertions.assertNotNull(applicationModelBeanFactory.getBean(FrameworkStatusReportService.class));
        Assertions.assertNotNull(applicationModelBeanFactory.getBean(ConfigurationCache.class));

        frameworkModel.destroy();
    }

    @Test
    public void testDefaultApplication() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        FrameworkModel frameworkModel = applicationModel.getFrameworkModel();

        Assertions.assertFalse(applicationModel.isInternal());
        Assertions.assertEquals(frameworkModel.defaultApplication(), applicationModel);
        Assertions.assertTrue(frameworkModel.getApplicationModels().contains(applicationModel));

        String desc = applicationModel.getDesc();
        Assertions.assertEquals(desc, "Dubbo Application[" + applicationModel.getInternalId() + "](unknown)");

        frameworkModel.destroy();
    }

    @Test
    public void testModule() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);

        ModuleModel defaultModule = applicationModel.getDefaultModule();
        ModuleModel internalModule = applicationModel.getInternalModule();

        Assertions.assertTrue(applicationModel.getModuleModels().contains(defaultModule));
        Assertions.assertTrue(applicationModel.getModuleModels().contains(internalModule));
        Assertions.assertTrue(applicationModel.getPubModuleModels().contains(defaultModule));
        Assertions.assertFalse(applicationModel.getPubModuleModels().contains(internalModule));

        applicationModel.removeModule(defaultModule);
        Assertions.assertFalse(applicationModel.getModuleModels().contains(defaultModule));
        Assertions.assertFalse(applicationModel.getPubModuleModels().contains(defaultModule));

        frameworkModel.destroy();
    }

    @Test
    public void testOfNullable() {
        ApplicationModel applicationModel = ApplicationModel.ofNullable(null);
        Assertions.assertEquals(ApplicationModel.defaultModel(), applicationModel);
        applicationModel.getFrameworkModel().destroy();

        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel1 = new ApplicationModel(frameworkModel);
        ApplicationModel applicationModel2 = ApplicationModel.ofNullable(applicationModel1);
        Assertions.assertEquals(applicationModel1, applicationModel2);
        frameworkModel.destroy();
    }

    @Test
    public void testDestroy() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);

        applicationModel.getDefaultModule();
        applicationModel.newModule();

        MockScopeModelDestroyListener destroyListener = new MockScopeModelDestroyListener();
        applicationModel.addDestroyListener(destroyListener);
        applicationModel.destroy();

        Assertions.assertFalse(frameworkModel.getApplicationModels().contains(applicationModel));
        Assertions.assertTrue(applicationModel.getModuleModels().isEmpty());
        Assertions.assertTrue(destroyListener.isDestroyed());
        Assertions.assertEquals(destroyListener.getScopeModel(), applicationModel);
        Assertions.assertNull(applicationModel.getApplicationServiceRepository());
        Assertions.assertTrue(applicationModel.isDestroyed());
        // trigger frameworkModel.tryDestroy()
        Assertions.assertTrue(frameworkModel.isDestroyed());

        try {
            applicationModel.getDefaultModule();
            Assertions.fail("Cannot create new module after application model destroyed");
        } catch (Exception e) {
            Assertions.assertEquals("ApplicationModel is destroyed", e.getMessage(), StringUtils.toString(e));
        }

        try {
            applicationModel.newModule();
            Assertions.fail("Cannot create new module after application model destroyed");
        } catch (Exception e) {
            Assertions.assertEquals("ApplicationModel is destroyed", e.getMessage(), StringUtils.toString(e));
        }

    }

}
