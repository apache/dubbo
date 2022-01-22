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

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.utils.StringUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link FrameworkModel}
 */
public class FrameworkModelTest {
    @Test
    public void testInitialize() {
        FrameworkModel.destroyAll();
        FrameworkModel frameworkModel = new FrameworkModel();

        Assertions.assertNull(frameworkModel.getParent());
        Assertions.assertEquals(frameworkModel.getScope(), ExtensionScope.FRAMEWORK);

        Assertions.assertNotNull(frameworkModel.getInternalId());
        Assertions.assertTrue(FrameworkModel.getAllInstances().contains(frameworkModel));
        Assertions.assertEquals(FrameworkModel.defaultModel(), frameworkModel);

        Assertions.assertNotNull(frameworkModel.getExtensionDirector());
        Assertions.assertNotNull(frameworkModel.getBeanFactory());
        Assertions.assertTrue(frameworkModel.getClassLoaders().contains(ScopeModel.class.getClassLoader()));

        Assertions.assertNotNull(frameworkModel.getServiceRepository());
        ApplicationModel applicationModel = frameworkModel.getInternalApplicationModel();
        Assertions.assertNotNull(applicationModel);
        Assertions.assertTrue(frameworkModel.getAllApplicationModels().contains(applicationModel));
        Assertions.assertFalse(frameworkModel.getApplicationModels().contains(applicationModel));

        frameworkModel.destroy();
    }

    @Test
    public void testDefaultModel() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        Assertions.assertTrue(FrameworkModel.getAllInstances().contains(frameworkModel));
        String desc = frameworkModel.getDesc();
        Assertions.assertEquals(desc, "Dubbo Framework[" + frameworkModel.getInternalId() + "]");
        frameworkModel.destroy();
    }

    @Test
    public void testApplicationModel() {
        FrameworkModel frameworkModel = new FrameworkModel();

        ApplicationModel applicationModel = frameworkModel.defaultApplication();
        ApplicationModel internalApplicationModel = frameworkModel.getInternalApplicationModel();

        Assertions.assertEquals(frameworkModel.getDefaultAppModel(), applicationModel);
        Assertions.assertTrue(frameworkModel.getAllApplicationModels().contains(applicationModel));
        Assertions.assertTrue(frameworkModel.getAllApplicationModels().contains(internalApplicationModel));
        Assertions.assertTrue(frameworkModel.getApplicationModels().contains(applicationModel));
        Assertions.assertFalse(frameworkModel.getApplicationModels().contains(internalApplicationModel));

        frameworkModel.removeApplication(applicationModel);
        Assertions.assertFalse(frameworkModel.getAllApplicationModels().contains(applicationModel));
        Assertions.assertFalse(frameworkModel.getApplicationModels().contains(applicationModel));

        frameworkModel.destroy();
    }

    @Test
    public void destroyAll() {
        FrameworkModel frameworkModel = new FrameworkModel();
        frameworkModel.defaultApplication();
        frameworkModel.newApplication();

        FrameworkModel.destroyAll();
        Assertions.assertTrue(FrameworkModel.getAllInstances().isEmpty());
        Assertions.assertTrue(frameworkModel.isDestroyed());

        try {
            frameworkModel.defaultApplication();
            Assertions.fail("Cannot create new application after framework model destroyed");
        } catch (Exception e) {
            Assertions.assertEquals("FrameworkModel is destroyed", e.getMessage(), StringUtils.toString(e));
        }

        try {
            frameworkModel.newApplication();
            Assertions.fail("Cannot create new application after framework model destroyed");
        } catch (Exception e) {
            Assertions.assertEquals("FrameworkModel is destroyed", e.getMessage(), StringUtils.toString(e));
        }
    }

}
