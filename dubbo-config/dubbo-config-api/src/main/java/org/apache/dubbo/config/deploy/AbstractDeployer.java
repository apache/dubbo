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
package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.deploy.Deployer;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.context.ModelContext;
import org.apache.dubbo.rpc.model.ScopeModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_MONITOR_EXCEPTION;
import static org.apache.dubbo.common.deploy.DeployState.FAILED;
import static org.apache.dubbo.common.deploy.DeployState.PENDING;
import static org.apache.dubbo.common.deploy.DeployState.STARTED;
import static org.apache.dubbo.common.deploy.DeployState.STARTING;
import static org.apache.dubbo.common.deploy.DeployState.STOPPED;
import static org.apache.dubbo.common.deploy.DeployState.STOPPING;

public abstract class AbstractDeployer<E extends ScopeModel> implements Deployer<E>{

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractDeployer.class);

    private E scopeModel;

    private ModelContext<E> modelContext;

    public AbstractDeployer(ModelContext<E> modelContext) {
        this.modelContext = modelContext;
    }

    @Override
    public boolean isPending() {
        return getState() == PENDING;
    }

    @Override
    public boolean isRunning() {
        return getState() == STARTING || getState() == STARTED;
    }

    @Override
    public boolean isStarted() {
        return getState() == STARTED;
    }

    @Override
    public boolean isStarting() {
        return getState() == STARTING;
    }

    @Override
    public boolean isStopping() {
        return getState() == STOPPING;
    }

    @Override
    public boolean isStopped() {
        return getState() == STOPPED;
    }

    @Override
    public boolean isFailed() {
        return getState() == FAILED;
    }

    @Override
    public DeployState getState() {
        return modelContext.getCurrentState();
    }

    @Override
    public void addDeployListener(DeployListener<E> listener) {
        modelContext.addDeployListener(listener);
    }

    @Override
    public void removeDeployListener(DeployListener<E> listener) {
        modelContext.removeDeployListener(listener);
    }

    public void setState(DeployState state){
        this.modelContext.setModelState(state);
    }

    public void setPending() {
        setState(PENDING);
    }

    protected void setStarting() {
       setState(STARTING);
        for (DeployListener<E> listener : modelContext.getListeners()) {
            try {
                listener.onStarting(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle starting event", e);
            }
        }
    }

    protected void setStarted() {
        setState(STARTED);
        for (DeployListener<E> listener : modelContext.getListeners()) {
            try {
                listener.onStarted(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle started event", e);
            }
        }
    }

    protected void setStopping() {
        setState(STOPPING);
        for (DeployListener<E> listener : modelContext.getListeners()) {
            try {
                listener.onStopping(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle stopping event", e);
            }
        }
    }

    protected void setStopped() {
        setState(STOPPED);
        for (DeployListener<E> listener : modelContext.getListeners()) {
            try {
                listener.onStopped(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle stopped event", e);
            }
        }
    }

    protected void setFailed(Throwable error) {
        setState(FAILED);
        this.modelContext.setLastError(error);
        for (DeployListener<E> listener :  modelContext.getListeners()) {
            try {
                listener.onFailure(scopeModel, error);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle failed event", e);
            }
        }
    }

    protected ModelContext<E> getModelContext(){
        return this.modelContext;
    }

    @Override
    public Throwable getError() {
        return this.modelContext.getLastError();
    }

    public boolean isInitialized() {
        return modelContext.initialized();
    }

    public String getIdentifier() {
        return scopeModel.getDesc();
    }
}
