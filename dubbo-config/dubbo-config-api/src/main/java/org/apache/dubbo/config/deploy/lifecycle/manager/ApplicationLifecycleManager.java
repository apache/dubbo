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

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.config.deploy.lifecycle.application.ApplicationLifecycle;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;


/**
 * Application lifecycle manager
 */
public class ApplicationLifecycleManager implements LifecycleManager{

    private final List<ApplicationLifecycle> sequences;

    private final ApplicationContext applicationContext;

    public ApplicationLifecycleManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        sequences = loadAll();
    }

    @Override
    public void start(){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.start(applicationContext));
    }

    @Override
    public void initialize() {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.initialize(applicationContext));
    }

    @Override
    public void preDestroy() {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.preDestroy(applicationContext));
    }

    @Override
    public void postDestroy() {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.postDestroy(applicationContext));
    }

    public void preModuleChanged(ModuleModel changedModule, DeployState moduleNewState) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.preModuleChanged(applicationContext,changedModule,moduleNewState));
    }

    public void postModuleChanged(ModuleModel changedModule, DeployState moduleNewState,DeployState applicationOldState,DeployState applicationNewState) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.postModuleChanged(applicationContext,changedModule,moduleNewState,applicationOldState,applicationNewState));
    }

    public void runRefreshServiceInstance(){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.refreshServiceInstance(applicationContext));
    }

    public List<ApplicationLifecycle> getAll(){
        return this.sequences;
    }

    protected List<ApplicationLifecycle> loadAll() {
        ExtensionLoader<ApplicationLifecycle> loader = applicationContext.getModel().getExtensionLoader(ApplicationLifecycle.class);
        List<ApplicationLifecycle> lifecycles = loader.getActivateExtensions();

        ScopeBeanFactory beanFactory = applicationContext.getModel().getBeanFactory();
        lifecycles.forEach(beanFactory::registerBean);

        return lifecycles;
    }
}
