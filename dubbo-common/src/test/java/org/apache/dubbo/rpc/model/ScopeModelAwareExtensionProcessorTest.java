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

import org.apache.dubbo.rpc.support.MockScopeModelAware;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ScopeModelAwareExtensionProcessor}
 */
public class ScopeModelAwareExtensionProcessorTest {
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
    public void testInitialize() {
        ScopeModelAwareExtensionProcessor processor1 = new ScopeModelAwareExtensionProcessor(frameworkModel);
        Assertions.assertEquals(processor1.getFrameworkModel(), frameworkModel);
        Assertions.assertEquals(processor1.getScopeModel(), frameworkModel);
        Assertions.assertNull(processor1.getApplicationModel());
        Assertions.assertNull(processor1.getModuleModel());

        ScopeModelAwareExtensionProcessor processor2 = new ScopeModelAwareExtensionProcessor(applicationModel);
        Assertions.assertEquals(processor2.getApplicationModel(), applicationModel);
        Assertions.assertEquals(processor2.getScopeModel(), applicationModel);
        Assertions.assertEquals(processor2.getFrameworkModel(), frameworkModel);
        Assertions.assertNull(processor2.getModuleModel());

        ScopeModelAwareExtensionProcessor processor3 = new ScopeModelAwareExtensionProcessor(moduleModel);
        Assertions.assertEquals(processor3.getModuleModel(), moduleModel);
        Assertions.assertEquals(processor3.getScopeModel(), moduleModel);
        Assertions.assertEquals(processor2.getApplicationModel(), applicationModel);
        Assertions.assertEquals(processor2.getFrameworkModel(), frameworkModel);
    }

    @Test
    public void testPostProcessAfterInitialization() throws Exception {
        ScopeModelAwareExtensionProcessor processor = new ScopeModelAwareExtensionProcessor(moduleModel);
        MockScopeModelAware mockScopeModelAware = new MockScopeModelAware();
        Object object = processor.postProcessAfterInitialization(mockScopeModelAware, mockScopeModelAware.getClass().getName());
        Assertions.assertEquals(object, mockScopeModelAware);

        Assertions.assertEquals(mockScopeModelAware.getScopeModel(), moduleModel);
        Assertions.assertEquals(mockScopeModelAware.getFrameworkModel(), frameworkModel);
        Assertions.assertEquals(mockScopeModelAware.getApplicationModel(), applicationModel);
        Assertions.assertEquals(mockScopeModelAware.getModuleModel(), moduleModel);
    }
}
