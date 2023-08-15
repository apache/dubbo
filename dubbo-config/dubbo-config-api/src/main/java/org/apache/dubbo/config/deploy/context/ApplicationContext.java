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
import org.apache.dubbo.config.utils.CompositeReferenceCache;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The lifecycle attribute aggregate of application.
 */
public class ApplicationContext extends AbstractModelContext<ApplicationModel> {

    private final AtomicBoolean hasPreparedApplicationInstance = new AtomicBoolean(false);

    private final AtomicBoolean registered = new AtomicBoolean(false);

    private volatile boolean hasPreparedInternalModule = false;

    private final ReferenceCache referenceCache;

    /**
     * Indicate that how many threads are updating service
     */
    private final AtomicInteger serviceRefreshState = new AtomicInteger(0);

    private final ApplicationLifecycleManager lifecycleManager;

    private final ExecutorRepository executorRepository;

    private final FrameworkExecutorRepository frameworkExecutorRepository;

    private final Environment environment;

    protected List<DeployListener<ApplicationModel>> listeners = new CopyOnWriteArrayList<>();

    public ApplicationContext(ApplicationModel applicationModel) {
        super(applicationModel);
        this.environment = applicationModel.modelEnvironment();
        this.referenceCache = new CompositeReferenceCache(applicationModel);
        this.frameworkExecutorRepository =  applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);
        this.executorRepository = ExecutorRepository.getInstance(applicationModel);
        this.lifecycleManager = new ApplicationLifecycleManager(this);
    }

    @Override
    public void runStart(){
        lifecycleManager.start();
    }

    @Override
    public void runInitialize(){
        lifecycleManager.initialize();
    }

    @Override
    public void runPreDestroy(){
        lifecycleManager.preDestroy();
    }

    @Override
    public void runPostDestroy(){
        lifecycleManager.postDestroy();
    }

    public boolean hasPreparedInternalModule() {
        return hasPreparedInternalModule;
    }

    public void setHasPreparedInternalModule(boolean hasPreparedInternalModule) {
        this.hasPreparedInternalModule = hasPreparedInternalModule;
    }

    public boolean isHasPreparedApplicationInstance() {
        return hasPreparedApplicationInstance.get();
    }

    public boolean isHasPreparedInternalModule() {
        return hasPreparedInternalModule;
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

    public void runRefreshServiceInstance(){
        lifecycleManager.runRefreshServiceInstance();
    }


    public void runPreModuleChanged(ModuleModel changedModule, DeployState newState){
        lifecycleManager.preModuleChanged(changedModule, newState);
    }

    public void runPostModuleChanged(ModuleModel changedModule,DeployState moduleNewState,DeployState applicationNewState,DeployState applicationOldState){
        lifecycleManager.postModuleChanged(changedModule, moduleNewState, applicationNewState, applicationOldState);
    }

}
