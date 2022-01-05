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

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ScopeModelUtil}
 */
public class ScopeModelUtilTest {
    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
        applicationModel = new ApplicationModel(frameworkModel);
        moduleModel = new ModuleModel(applicationModel);
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    public void test() {

        Assertions.assertEquals(ScopeModelUtil.getFrameworkModel(null), FrameworkModel.defaultModel());
        Assertions.assertEquals(ScopeModelUtil.getFrameworkModel(frameworkModel), frameworkModel);
        Assertions.assertEquals(ScopeModelUtil.getFrameworkModel(applicationModel), frameworkModel);
        Assertions.assertEquals(ScopeModelUtil.getFrameworkModel(moduleModel), frameworkModel);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScopeModelUtil.getFrameworkModel(new MockScopeModel(null, null)));

        Assertions.assertEquals(ScopeModelUtil.getApplicationModel(null), ApplicationModel.defaultModel());
        Assertions.assertEquals(ScopeModelUtil.getApplicationModel(applicationModel), applicationModel);
        Assertions.assertEquals(ScopeModelUtil.getApplicationModel(moduleModel), applicationModel);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScopeModelUtil.getApplicationModel(frameworkModel));

        Assertions.assertEquals(ScopeModelUtil.getModuleModel(null), ApplicationModel.defaultModel().getDefaultModule());
        Assertions.assertEquals(ScopeModelUtil.getModuleModel(moduleModel), moduleModel);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScopeModelUtil.getModuleModel(frameworkModel));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScopeModelUtil.getModuleModel(applicationModel));

        Assertions.assertEquals(ScopeModelUtil.getOrDefault(null, SPIDemo1.class), FrameworkModel.defaultModel());
        Assertions.assertEquals(ScopeModelUtil.getOrDefault(null, SPIDemo2.class), ApplicationModel.defaultModel());
        Assertions.assertEquals(ScopeModelUtil.getOrDefault(null, SPIDemo3.class), ApplicationModel.defaultModel().getDefaultModule());
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScopeModelUtil.getOrDefault(null, SPIDemo4.class));

        Assertions.assertEquals(ScopeModelUtil.getExtensionLoader(SPIDemo1.class, null), FrameworkModel.defaultModel().getExtensionLoader(SPIDemo1.class));
        Assertions.assertEquals(ScopeModelUtil.getExtensionLoader(SPIDemo2.class, null), ApplicationModel.defaultModel().getExtensionLoader(SPIDemo2.class));
        Assertions.assertEquals(ScopeModelUtil.getExtensionLoader(SPIDemo3.class, null), ApplicationModel.defaultModel().getDefaultModule().getExtensionLoader(SPIDemo3.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScopeModelUtil.getExtensionLoader(SPIDemo4.class, null));
    }


    @SPI(scope = ExtensionScope.FRAMEWORK)
    interface SPIDemo1 {

    }

    @SPI(scope = ExtensionScope.APPLICATION)
    interface SPIDemo2 {

    }

    @SPI(scope = ExtensionScope.MODULE)
    interface SPIDemo3 {

    }

    interface SPIDemo4 {

    }

    class MockScopeModel extends ScopeModel {
        public MockScopeModel(ScopeModel parent, ExtensionScope scope) {
            super(parent, scope);
        }

        @Override
        protected void onDestroy() {

        }

        @Override
        public Environment getModelEnvironment() {
            return null;
        }
    }

}

