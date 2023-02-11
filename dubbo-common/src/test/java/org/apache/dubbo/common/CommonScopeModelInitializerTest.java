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
package org.apache.dubbo.common;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.config.ConfigurationCache;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link CommonScopeModelInitializer}
 */
class CommonScopeModelInitializerTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
        applicationModel = frameworkModel.newApplication();
        moduleModel = applicationModel.newModule();
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    void test() {
        ScopeBeanFactory applicationModelBeanFactory = applicationModel.getBeanFactory();
        Assertions.assertNotNull(applicationModelBeanFactory.getBean(ShutdownHookCallbacks.class));
        Assertions.assertNotNull(applicationModelBeanFactory.getBean(FrameworkStatusReportService.class));
        Assertions.assertNotNull(applicationModelBeanFactory.getBean(ConfigurationCache.class));

        ScopeBeanFactory moduleModelBeanFactory = moduleModel.getBeanFactory();
        Assertions.assertNotNull(moduleModelBeanFactory.getBean(ConfigurationCache.class));
    }
}
