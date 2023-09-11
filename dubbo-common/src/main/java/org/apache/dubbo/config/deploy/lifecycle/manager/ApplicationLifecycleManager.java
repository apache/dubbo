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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.config.deploy.lifecycle.application.ApplicationLifecycle;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;


/**
 * Application lifecycle manager
 */
public class ApplicationLifecycleManager{

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ApplicationLifecycleManager.class);

    private final List<ApplicationLifecycle> sequences;

    private final ApplicationModel applicationModel;

    public ApplicationLifecycleManager(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        sequences = loadAll();
    }

    public void start(ApplicationContext applicationContext){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.start(applicationContext));
    }

    public void initialize(ApplicationContext applicationContext) {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.initialize(applicationContext));
    }

    public void preDestroy(ApplicationContext applicationContext) {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.preDestroy(applicationContext));
    }

    public void postDestroy(ApplicationContext applicationContext) {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.postDestroy(applicationContext));
    }

    public void preModuleChanged(ApplicationContext applicationContext,ModuleModel changedModule, DeployState moduleNewState) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.preModuleChanged(applicationContext,changedModule,moduleNewState));
    }

    public void postModuleChanged(ApplicationContext applicationContext,ModuleModel changedModule, DeployState moduleNewState,DeployState applicationOldState,DeployState applicationNewState) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.postModuleChanged(applicationContext,changedModule,moduleNewState,applicationOldState,applicationNewState));
    }

    public void runRefreshServiceInstance(ApplicationContext applicationContext){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.refreshServiceInstance(applicationContext));
    }

    public List<ApplicationLifecycle> getAll(){
        return this.sequences;
    }

    protected List<ApplicationLifecycle> loadAll() {
        ExtensionLoader<ApplicationLifecycle> loader = applicationModel.getExtensionLoader(ApplicationLifecycle.class);
        List<ApplicationLifecycle> lifecycles = loader.getActivateExtensions();

        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        StringBuilder sequence = new StringBuilder("Loaded lifecycle sequences: [START]-> ");

        lifecycles.forEach(applicationLifecycle -> {
            if(applicationLifecycle.needInitialize()) {
                beanFactory.registerBean(applicationLifecycle);
                sequence.append(applicationLifecycle.getClass().getSimpleName()).append("->");
            }
        });
        sequence.append(" [END]");
        logger.info(sequence.toString());

        return lifecycles;
    }
}
