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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.probe.StartupProbe;
import org.apache.dubbo.qos.probe.impl.DeployerStartupProbe;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class StartupTest {
    private FrameworkModel frameworkModel;
    private ModuleDeployer moduleDeployer;

    @BeforeEach
    public void setUp() {
        frameworkModel = Mockito.mock(FrameworkModel.class);
        ApplicationModel applicationModel = Mockito.mock(ApplicationModel.class);
        ModuleModel moduleModel = Mockito.mock(ModuleModel.class);
        ConfigManager manager = Mockito.mock(ConfigManager.class);
        Mockito.when(manager.getApplication()).thenReturn(Optional.of(new ApplicationConfig("ReadyTest")));
        moduleDeployer = Mockito.mock(ModuleDeployer.class);
        Mockito.when(frameworkModel.newApplication()).thenReturn(applicationModel);
        Mockito.when(frameworkModel.getApplicationModels()).thenReturn(Arrays.asList(applicationModel));
        Mockito.when(applicationModel.getModuleModels()).thenReturn(Arrays.asList(moduleModel));
        Mockito.when(applicationModel.getApplicationConfigManager()).thenReturn(manager);
        Mockito.when(moduleModel.getDeployer()).thenReturn(moduleDeployer);
        Mockito.when(moduleDeployer.isRunning()).thenReturn(true);

        ExtensionLoader loader = Mockito.mock(ExtensionLoader.class);
        Mockito.when(frameworkModel.getExtensionLoader(StartupProbe.class)).thenReturn(loader);
        URL url = URL.valueOf("application://").addParameter(CommonConstants.QOS_STARTUP_PROBE_EXTENSION, "");
        List<StartupProbe> readinessProbes = Arrays.asList(
            new DeployerStartupProbe(frameworkModel)
        );
        Mockito.when(loader.getActivateExtension(url, CommonConstants.QOS_STARTUP_PROBE_EXTENSION)).thenReturn(readinessProbes);
    }

    @Test
    void testExecute() {
        Startup startup = new Startup(frameworkModel);
        CommandContext commandContext = new CommandContext("startup");

        String result = startup.execute(commandContext, new String[0]);
        Assertions.assertEquals("true", result);
        Assertions.assertEquals(commandContext.getHttpCode(), 200);

        Mockito.when(moduleDeployer.isRunning()).thenReturn(false);
        result = startup.execute(commandContext, new String[0]);
        Assertions.assertEquals("false", result);
        Assertions.assertEquals(commandContext.getHttpCode(), 503);
    }
}
