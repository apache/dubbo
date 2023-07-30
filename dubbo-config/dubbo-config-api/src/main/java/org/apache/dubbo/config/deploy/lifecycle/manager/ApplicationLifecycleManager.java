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
package org.apache.dubbo.config.deploy.lifecycle.manager;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application Life Manager Loader
 */
public class ApplicationLifecycleManager{

    private final DefaultApplicationDeployer defaultApplicationDeployer;

    private List<ApplicationLifecycle> sequences;

    public ApplicationLifecycleManager(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.defaultApplicationDeployer = defaultApplicationDeployer;
        sequences = loadAll();
        sequences.forEach(applicationLifecycle -> applicationLifecycle.setApplicationDeployer(this.defaultApplicationDeployer));
    }

    public void start(AtomicBoolean hasPreparedApplicationInstance){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.start(hasPreparedApplicationInstance));
    }

    public void initialize() {
        getAll().forEach(ApplicationLifecycle::initialize);
    }

    public void preDestroy() {
        getAll().forEach(ApplicationLifecycle::preDestroy);
    }

    public void postDestroy() {
        getAll().forEach(ApplicationLifecycle::postDestroy);
    }

    public void preModuleChanged(ModuleModel changedModule, DeployState changedModuleState, AtomicBoolean hasPreparedApplicationInstance) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.preModuleChanged(changedModule, changedModuleState, hasPreparedApplicationInstance));
    }

    public void postModuleChanged(ModuleModel changedModule, DeployState changedModuleState, DeployState applicationNewState) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.postModuleChanged(changedModule, changedModuleState, applicationNewState));
    }

    public void runRefreshServiceInstance(){
        getAll().forEach(ApplicationLifecycle::refreshServiceInstance);
    }

    public List<ApplicationLifecycle> getAll(){
        return this.sequences;
    }


    protected List<ApplicationLifecycle> loadAll() {
        ExtensionLoader<ApplicationLifecycle> loader = defaultApplicationDeployer.getApplicationModel().getExtensionLoader(ApplicationLifecycle.class);
        return loader.getActivateExtensions();
    }
}
