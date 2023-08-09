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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;

import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Application config pre-handle lifecycle.
 */
@Activate(order = -4000)
public class ApplicationConfigPreHandleLifecycle implements ApplicationLifecycle{

    private DefaultApplicationDeployer defaultApplicationDeployer;

    @Override
    public void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.defaultApplicationDeployer = defaultApplicationDeployer;
    }

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public void initialize() {
        ConfigManager configManager = defaultApplicationDeployer.getApplicationModel().getApplicationConfigManager();
        configManager.loadConfigsOfTypeFromProps(ApplicationConfig.class);

        ApplicationModel applicationModel = defaultApplicationDeployer.getApplicationModel();
        // try set model name
        if (StringUtils.isBlank(applicationModel.getModelName())) {
            applicationModel.setModelName(applicationModel.tryGetApplicationName());
        }
    }
}
