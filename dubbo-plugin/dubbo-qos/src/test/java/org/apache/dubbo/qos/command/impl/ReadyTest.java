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
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.probe.ReadinessProbe;
import org.apache.dubbo.qos.probe.impl.DeployerReadinessProbe;
import org.apache.dubbo.qos.probe.impl.ProviderReadinessProbe;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class ReadyTest {

    private FrameworkModel frameworkModel;
    private ModuleDeployer moduleDeployer;
    private FrameworkServiceRepository frameworkServiceRepository;

    @BeforeEach
    public void setUp() {
        frameworkModel = Mockito.mock(FrameworkModel.class);
        frameworkServiceRepository = Mockito.mock(FrameworkServiceRepository.class);
        ConfigManager manager = Mockito.mock(ConfigManager.class);
        Mockito.when(manager.getApplication()).thenReturn(Optional.of(new ApplicationConfig("ReadyTest")));
        ApplicationModel applicationModel = Mockito.mock(ApplicationModel.class);
        ModuleModel moduleModel = Mockito.mock(ModuleModel.class);
        moduleDeployer = Mockito.mock(ModuleDeployer.class);
        Mockito.when(frameworkServiceRepository.allProviderModels()).thenReturn(Collections.emptyList());
        Mockito.when(frameworkModel.newApplication()).thenReturn(applicationModel);
        Mockito.when(frameworkModel.getApplicationModels()).thenReturn(Arrays.asList(applicationModel));
        Mockito.when(frameworkModel.getServiceRepository()).thenReturn(frameworkServiceRepository);
        Mockito.when(applicationModel.getModuleModels()).thenReturn(Arrays.asList(moduleModel));
        Mockito.when(applicationModel.getApplicationConfigManager()).thenReturn(manager);
        Mockito.when(moduleModel.getDeployer()).thenReturn(moduleDeployer);
        Mockito.when(moduleDeployer.isStarted()).thenReturn(true);

        ExtensionLoader loader = Mockito.mock(ExtensionLoader.class);
        Mockito.when(frameworkModel.getExtensionLoader(ReadinessProbe.class)).thenReturn(loader);
        URL url = URL.valueOf("application://").addParameter(CommonConstants.QOS_READY_PROBE_EXTENSION, "");
        List<ReadinessProbe> readinessProbes = Arrays.asList(
            new DeployerReadinessProbe(frameworkModel),
            new ProviderReadinessProbe(frameworkModel)
        );
        Mockito.when(loader.getActivateExtension(url, CommonConstants.QOS_READY_PROBE_EXTENSION)).thenReturn(readinessProbes);
    }

    @Test
    void testExecute() {
        Ready ready = new Ready(frameworkModel);
        CommandContext commandContext = new CommandContext("ready");

        String result = ready.execute(commandContext, new String[0]);
        Assertions.assertEquals("true", result);
        Assertions.assertEquals(commandContext.getHttpCode(), 200);

        Mockito.when(moduleDeployer.isStarted()).thenReturn(false);
        result = ready.execute(commandContext, new String[0]);
        Assertions.assertEquals("false", result);
        Assertions.assertEquals(commandContext.getHttpCode(), 503);
    }
}