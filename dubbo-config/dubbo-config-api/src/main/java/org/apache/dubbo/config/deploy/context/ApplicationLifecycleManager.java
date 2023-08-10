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
package org.apache.dubbo.config.deploy.context;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle;
import org.apache.dubbo.config.deploy.lifecycle.event.AppInitEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPostDestroyEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPostModuleChangeEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPreDestroyEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPreModuleChangeEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppServiceRefreshEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppStartEvent;


import java.util.List;


/**
 * Application Life Manager Loader
 */
public class ApplicationLifecycleManager{

    private final List<ApplicationLifecycle> sequences;

    private final ApplicationContext applicationContext;

    public ApplicationLifecycleManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        sequences = loadAll();
    }

    public void start(AppStartEvent appStartEvent){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.start(appStartEvent));
    }

    public void initialize(AppInitEvent appInitEvent) {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.initialize(appInitEvent));
    }

    public void preDestroy(AppPreDestroyEvent appPreDestroyEvent) {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.preDestroy(appPreDestroyEvent));
    }

    public void postDestroy(AppPostDestroyEvent appPostDestroyEvent) {
        getAll().forEach(applicationLifecycle-> applicationLifecycle.postDestroy(appPostDestroyEvent));
    }

    public void preModuleChanged(AppPreModuleChangeEvent preModuleChangeEvent) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.preModuleChanged(preModuleChangeEvent));
    }

    public void postModuleChanged(AppPostModuleChangeEvent postModuleChangeEvent) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.postModuleChanged(postModuleChangeEvent));
    }

    public void runRefreshServiceInstance(AppServiceRefreshEvent serviceRefreshEvent){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.refreshServiceInstance(serviceRefreshEvent));
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
