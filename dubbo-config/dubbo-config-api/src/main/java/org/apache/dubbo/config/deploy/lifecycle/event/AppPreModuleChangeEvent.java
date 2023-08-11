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
package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @see org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle#preModuleChanged(AppPreModuleChangeEvent) 
 */
public class AppPreModuleChangeEvent extends AbstractApplicationEvent{

    private final ModuleModel changedModule;

    private final DeployState moduleState;

    private final AtomicBoolean hasPreparedApplicationInstance;

    private final AtomicBoolean registered;

    private final AtomicInteger serviceRefreshState;

    public AppPreModuleChangeEvent(ApplicationModel applicationModel,DeployState applicationCurrentState,ModuleModel changedModule, DeployState moduleState, AtomicBoolean hasPreparedApplicationInstance, AtomicBoolean registered,AtomicInteger serviceRefreshState) {
        super(applicationModel,applicationCurrentState);
        this.changedModule = changedModule;
        this.moduleState = moduleState;
        this.hasPreparedApplicationInstance = hasPreparedApplicationInstance;
        this.registered = registered;
        this.serviceRefreshState =serviceRefreshState;
    }

    public ModuleModel getChangedModule() {
        return changedModule;
    }

    public DeployState getModuleState() {
        return moduleState;
    }

    public AtomicBoolean getHasPreparedApplicationInstance() {
        return hasPreparedApplicationInstance;
    }

    public AtomicBoolean registered() {
        return registered;
    }

    public AtomicInteger getServiceRefreshState() {
        return serviceRefreshState;
    }
}
