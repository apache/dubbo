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
import org.mockito.Mockito;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;

/**
 *
 */
class ConfigurationUtilsTest {
    @Test
    void testCachedProperties() {
        FrameworkModel frameworkModel = new FrameworkModel();

        ApplicationModel applicationModel = frameworkModel.newApplication();
        Environment originApplicationEnvironment = applicationModel.getModelEnvironment();
        Environment applicationEnvironment = Mockito.spy(originApplicationEnvironment);
        applicationModel.setEnvironment(applicationEnvironment);

        Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(applicationEnvironment.getDynamicGlobalConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.getString("TestKey", "")).thenReturn("a");

        Assertions.assertEquals("a", ConfigurationUtils.getCachedDynamicProperty(applicationModel, "TestKey", "xxx"));

        Mockito.when(configuration.getString("TestKey", "")).thenReturn("b");
        // cached key
        Assertions.assertEquals("a", ConfigurationUtils.getCachedDynamicProperty(applicationModel, "TestKey", "xxx"));

        ModuleModel moduleModel = applicationModel.newModule();
        ModuleEnvironment originModuleEnvironment = moduleModel.getModelEnvironment();
        ModuleEnvironment moduleEnvironment = Mockito.spy(originModuleEnvironment);
        moduleModel.setModuleEnvironment(moduleEnvironment);

        Mockito.when(moduleEnvironment.getDynamicGlobalConfiguration()).thenReturn(configuration);

        // ApplicationModel should not affect ModuleModel
        Assertions.assertEquals("b", ConfigurationUtils.getCachedDynamicProperty(moduleModel, "TestKey", "xxx"));

        Mockito.when(configuration.getString("TestKey", "")).thenReturn("c");
        // cached key
        Assertions.assertEquals("b", ConfigurationUtils.getCachedDynamicProperty(moduleModel, "TestKey", "xxx"));

        moduleModel.setModuleEnvironment(originModuleEnvironment);
        applicationModel.setEnvironment(originApplicationEnvironment);

        frameworkModel.destroy();
    }

    @Test
    void testGetServerShutdownTimeout () {
        System.setProperty(SHUTDOWN_WAIT_KEY, " 10000");
        Assertions.assertEquals(10000, ConfigurationUtils.getServerShutdownTimeout(ApplicationModel.defaultModel()));
        System.clearProperty(SHUTDOWN_WAIT_KEY);
    }

    @Test
    void testGetProperty () {
        System.setProperty(SHUTDOWN_WAIT_KEY, " 10000");
        Assertions.assertEquals("10000", ConfigurationUtils.getProperty(ApplicationModel.defaultModel(), SHUTDOWN_WAIT_KEY));
        System.clearProperty(SHUTDOWN_WAIT_KEY);
    }

    @Test
    void testParseSingleProperties() throws Exception {
        String p1 = "aaa=bbb";
        Map<String, String> result = ConfigurationUtils.parseProperties(p1);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("bbb", result.get("aaa"));
    }

    @Test
    void testParseMultipleProperties() throws Exception {
        String p1 = "aaa=bbb\nccc=ddd";
        Map<String, String> result = ConfigurationUtils.parseProperties(p1);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("bbb", result.get("aaa"));
        Assertions.assertEquals("ddd", result.get("ccc"));
    }

    @Test
    void testEscapedNewLine() throws Exception {
        String p1 = "dubbo.registry.address=zookeeper://127.0.0.1:2181\\\\ndubbo.protocol.port=20880";
        Map<String, String> result = ConfigurationUtils.parseProperties(p1);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("zookeeper://127.0.0.1:2181\\ndubbo.protocol.port=20880", result.get("dubbo.registry.address"));
    }
}
