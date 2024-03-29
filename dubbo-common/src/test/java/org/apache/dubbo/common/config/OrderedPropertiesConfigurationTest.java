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
package org.apache.dubbo.common.config;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link OrderedPropertiesConfiguration}
 */
class OrderedPropertiesConfigurationTest {

    @Test
    void testOrderPropertiesProviders() {
        OrderedPropertiesConfiguration configuration = new OrderedPropertiesConfiguration(
                ApplicationModel.defaultModel().getDefaultModule());
        Assertions.assertEquals("999", configuration.getInternalProperty("testKey"));
    }

    @Test
    void testGetPropertyFromOrderedPropertiesConfiguration() {
        FrameworkModel frameworkModel = new FrameworkModel();

        ApplicationModel applicationModel = frameworkModel.newApplication();

        ModuleModel moduleModel = applicationModel.newModule();
        ModuleEnvironment moduleEnvironment = moduleModel.modelEnvironment();

        Configuration configuration = moduleEnvironment.getDynamicGlobalConfiguration();
        // MockOrderedPropertiesProvider2  initProperties
        Assertions.assertEquals("999", configuration.getString("testKey"));
    }
}
