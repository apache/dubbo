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
package org.apache.dubbo.xds.config;

import org.apache.dubbo.common.deploy.ModuleDeployListener;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Collection;
import java.util.List;

public class XdsModuleDeployListener implements ModuleDeployListener {
    @Override
    public void onInitialize(ModuleModel scopeModel) {
    }

    @Override
    public void onStarting(ModuleModel scopeModel) {
        // Optional<ModuleConfig> moduleConfig = scopeModel.getConfigManager().getModule();
        ModuleConfig moduleConfig = scopeModel
                .getConfigManager()
                .getModule()
                .orElseThrow(() -> new IllegalStateException("Default module config is not initialized"));
        List<RegistryConfig> registryConfigs = moduleConfig.getRegistries();
        if (registryConfigs != null) {
            String protocol = registryConfigs.get(0).getProtocol();
            System.out.println(protocol);
        }
        ModuleConfigManager configManager = scopeModel.getConfigManager();
        ConfigManager manager = scopeModel.getApplicationModel().getApplicationConfigManager();
        Collection<RegistryConfig> configs = manager.getRegistries();


        Collection<ConsumerConfig> consumerConfigs = configManager.getConsumers();
        for (ReferenceConfigBase<?> rc : configManager.getReferences()) {
            System.out.println(rc);
        }
    }

    @Override
    public void onStarted(ModuleModel scopeModel) {
    }

    @Override
    public void onStopping(ModuleModel scopeModel) {}

    @Override
    public void onStopped(ModuleModel scopeModel) {}

    @Override
    public void onFailure(ModuleModel scopeModel, Throwable cause) {}
}
