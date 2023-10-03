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

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.deploy.lifecycle.manager.ApplicationLifecycleManager;

import org.apache.dubbo.rpc.model.ApplicationModel;


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The lifecycle attribute aggregate of application.
 */
public class ApplicationContext {

    private final ApplicationModel applicationModel;

    private final AtomicBoolean hasPreparedApplicationInstance;

    private final AtomicBoolean registered ;

    private final AtomicBoolean hasPreparedInternalModule;

    private final ReferenceCache referenceCache;

    /**
     * Indicate that how many threads are updating service
     */
    private final AtomicInteger serviceRefreshState;

    private final ApplicationLifecycleManager lifecycleManager;

    private final ExecutorRepository executorRepository;

    private final FrameworkExecutorRepository frameworkExecutorRepository;

    private final Environment environment;

    private final ApplicationModel scopeModel;

    private final AtomicReference<DeployState> modelState;

    private final AtomicBoolean initialized;

    private final AtomicReference<Throwable> lastError;

    private final List<DeployListener<ApplicationModel>> deployListeners;

    public ApplicationContext(ApplicationModel applicationModel, AtomicBoolean hasPreparedApplicationInstance, AtomicBoolean registered, AtomicBoolean hasPreparedInternalModule, ReferenceCache referenceCache, AtomicInteger serviceRefreshState, ApplicationLifecycleManager lifecycleManager, ExecutorRepository executorRepository, FrameworkExecutorRepository frameworkExecutorRepository, Environment environment, ApplicationModel scopeModel, AtomicReference<DeployState> modelState, AtomicBoolean initialized, AtomicReference<Throwable> lastError,List<DeployListener<ApplicationModel>> deployListeners) {
        this.applicationModel = applicationModel;
        this.hasPreparedApplicationInstance = hasPreparedApplicationInstance;
        this.registered = registered;
        this.hasPreparedInternalModule = hasPreparedInternalModule;
        this.referenceCache = referenceCache;
        this.serviceRefreshState = serviceRefreshState;
        this.lifecycleManager = lifecycleManager;
        this.executorRepository = executorRepository;
        this.frameworkExecutorRepository = frameworkExecutorRepository;
        this.environment = environment;
        this.scopeModel = scopeModel;
        this.modelState = modelState;
        this.initialized = initialized;
        this.lastError = lastError;
        this.deployListeners = deployListeners;
    }

    public ApplicationModel getModel() {
        return applicationModel;
    }

    public void setModelState(DeployState newState) {
        this.modelState.set(newState);
    }

    public DeployState getCurrentState() {
        return this.modelState.get();
    }

    public Throwable getLastError() {
        return lastError.get();
    }

    public void setLastError(Throwable lastError) {
        this.lastError.set(lastError);
    }

    public boolean initialized() {
        return initialized.get();
    }

    public void setInitialized(boolean initialized) {
        this.initialized.set(initialized);
    }

    public boolean hasPreparedInternalModule() {
        return hasPreparedInternalModule.get();
    }

    public void setHasPreparedInternalModule(boolean hasPreparedInternalModule) {
        this.hasPreparedInternalModule.set(hasPreparedInternalModule);
    }

    public boolean isHasPreparedApplicationInstance() {
        return hasPreparedApplicationInstance.get();
    }

    public boolean isHasPreparedInternalModule() {
        return hasPreparedInternalModule.get();
    }

    public AtomicBoolean getHasPreparedApplicationInstance() {
        return hasPreparedApplicationInstance;
    }

    public void setRegistered(boolean registered){
        this.registered.set(registered);
    }

    public boolean registered() {
        return registered.get();
    }

    public AtomicBoolean getRegistered(){
        return registered;
    }

    public ReferenceCache getReferenceCache() {
        return referenceCache;
    }

    public AtomicInteger getServiceRefreshState() {
        return serviceRefreshState;
    }

    public ExecutorRepository getExecutorRepository() {
        return executorRepository;
    }

    public List<DeployListener<ApplicationModel>> getDeployListeners() {
        return deployListeners;
    }
}
