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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.PilotExchanger;

import java.util.Collection;

import static org.apache.dubbo.rpc.Constants.SUPPORT_MESH_TYPE;

public class XdsApplicationDeployListener implements ApplicationDeployListener {
    @Override
    public void onInitialize(ApplicationModel scopeModel) {
        System.out.println("hello");
    }

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        Collection<RegistryConfig> registryConfigs =
                scopeModel.getApplicationConfigManager().getRegistries();
        for (RegistryConfig registryConfig : registryConfigs) {
            String protocol = registryConfig.getProtocol();
            if (StringUtils.isNotEmpty(protocol) && SUPPORT_MESH_TYPE.contains(protocol)) {
                URL url = URL.valueOf(registryConfig.getAddress());
                url.setScopeModel(scopeModel);
                scopeModel.getBeanFactory().registerBean(PilotExchanger.createInstance(url));
                break;
            }
        }
    }

    @Override
    public void onStarted(ApplicationModel scopeModel) {
        System.out.println("hello");
    }

    @Override
    public void onStopping(ApplicationModel scopeModel) {}

    @Override
    public void onStopped(ApplicationModel scopeModel) {}

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {}
}
