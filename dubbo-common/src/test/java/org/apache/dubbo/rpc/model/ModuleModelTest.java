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
import org.apache.dubbo.common.config.ModuleEnvironment;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.rpc.support.MockScopeModelDestroyListener;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link ModuleModel}
 */
public class ModuleModelTest {

    @Test
    public void testInitialize() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);
        ModuleModel moduleModel = new ModuleModel(applicationModel);
        Assertions.assertEquals(moduleModel.getParent(), applicationModel);
        Assertions.assertEquals(moduleModel.getScope(), ExtensionScope.MODULE);
        Assertions.assertEquals(moduleModel.getApplicationModel(), applicationModel);
        Assertions.assertTrue(applicationModel.getPubModuleModels().contains(moduleModel));
        Assertions.assertNotNull(moduleModel.getInternalId());

        Assertions.assertNotNull(moduleModel.getExtensionDirector());
        Assertions.assertNotNull(moduleModel.getBeanFactory());
        Assertions.assertTrue(moduleModel.getClassLoaders().contains(ScopeModel.class.getClassLoader()));

        Assertions.assertNotNull(moduleModel.getServiceRepository());
        Assertions.assertNotNull(moduleModel.getConfigManager());

        ScopeBeanFactory moduleModelBeanFactory = moduleModel.getBeanFactory();
        Assertions.assertNotNull(moduleModelBeanFactory.getBean(ConfigurationCache.class));

        frameworkModel.destroy();
    }

    @Test
    public void testModelEnvironment() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);
        ModuleModel moduleModel = new ModuleModel(applicationModel);

        ModuleEnvironment modelEnvironment = moduleModel.getModelEnvironment();
        Assertions.assertNotNull(modelEnvironment);

        frameworkModel.destroy();
    }

    @Test
    public void testDestroy() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);
        ModuleModel moduleModel = new ModuleModel(applicationModel);

        MockScopeModelDestroyListener destroyListener = new MockScopeModelDestroyListener();
        moduleModel.addDestroyListener(destroyListener);

        moduleModel.destroy();
        Assertions.assertTrue(destroyListener.isDestroyed());
        Assertions.assertEquals(destroyListener.getScopeModel(), moduleModel);
        Assertions.assertFalse(applicationModel.getPubModuleModels().contains(moduleModel));
        Assertions.assertNull(moduleModel.getServiceRepository());
        Assertions.assertTrue(moduleModel.isDestroyed());

        // trigger tryDestroy
        Assertions.assertTrue(applicationModel.isDestroyed());
        Assertions.assertTrue(frameworkModel.isDestroyed());

    }
}
