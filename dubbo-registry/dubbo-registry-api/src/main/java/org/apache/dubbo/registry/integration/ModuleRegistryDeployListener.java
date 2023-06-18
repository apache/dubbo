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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.deploy.ModuleDeployListener;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;

import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;

public class ModuleRegistryDeployListener implements ModuleDeployListener {
    @Override
    public void onInitialize(ModuleModel scopeModel) {

    }

    @Override
    public void onStarting(ModuleModel scopeModel) {

    }

    @Override
    public void onStarted(ModuleModel scopeModel) {

    }

    @Override
    public void onStopping(ModuleModel moduleModel) {
        List<RegistryProtocolListener> listeners = moduleModel.getExtensionLoader(RegistryProtocolListener.class)
            .getLoadedExtensionInstances();
        if (CollectionUtils.isNotEmpty(listeners)) {
            for (RegistryProtocolListener listener : listeners) {
                listener.onDestroy();
            }
        }


        if (moduleModel.getApplicationModel().getModelEnvironment().getConfiguration().convert(Boolean.class, org.apache.dubbo.registry.Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            String applicationName = moduleModel.getApplicationModel().tryGetApplicationName();
            if (applicationName == null) {
                // already removed
                return;
            }
            if (moduleModel.getServiceRepository().getExportedServices().size() > 0) {
                RegistryProtocol.ProviderConfigurationListener providerConfigurationListener =
                    moduleModel.getBeanFactory().getBean(RegistryProtocol.ProviderConfigurationListener.class);
                if (providerConfigurationListener != null) {
                    moduleModel.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension()
                        .removeListener(applicationName + CONFIGURATORS_SUFFIX,
                            providerConfigurationListener);
                }
            }
        }
    }

    @Override
    public void onStopped(ModuleModel scopeModel) {

    }

    @Override
    public void onFailure(ModuleModel scopeModel, Throwable cause) {

    }
}
