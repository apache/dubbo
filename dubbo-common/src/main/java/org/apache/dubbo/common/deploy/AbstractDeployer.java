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
package org.apache.dubbo.common.deploy;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_MONITOR_EXCEPTION;
import static org.apache.dubbo.common.deploy.DeployState.FAILED;
import static org.apache.dubbo.common.deploy.DeployState.PENDING;
import static org.apache.dubbo.common.deploy.DeployState.STARTED;
import static org.apache.dubbo.common.deploy.DeployState.STARTING;
import static org.apache.dubbo.common.deploy.DeployState.STOPPED;
import static org.apache.dubbo.common.deploy.DeployState.STOPPING;

public abstract class AbstractDeployer<E extends ScopeModel> implements Deployer<E> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractDeployer.class);

    private volatile DeployState state = PENDING;

    private volatile Throwable lastError;

    protected volatile boolean initialized = false;

    protected List<DeployListener<E>> listeners = new CopyOnWriteArrayList<>();

    private E scopeModel;

    public AbstractDeployer(E scopeModel) {
        this.scopeModel = scopeModel;
    }

    @Override
    public boolean isPending() {
        return state == PENDING;
    }

    @Override
    public boolean isRunning() {
        return state == STARTING || state == STARTED;
    }

    @Override
    public boolean isStarted() {
        return state == STARTED;
    }

    @Override
    public boolean isStarting() {
        return state == STARTING;
    }

    @Override
    public boolean isStopping() {
        return state == STOPPING;
    }

    @Override
    public boolean isStopped() {
        return state == STOPPED;
    }

    @Override
    public boolean isFailed() {
        return state == FAILED;
    }

    @Override
    public DeployState getState() {
        return state;
    }

    @Override
    public void addDeployListener(DeployListener<E> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeDeployListener(DeployListener<E> listener) {
        listeners.remove(listener);
    }

    public void setPending() {
        this.state = PENDING;
    }

    protected void setStarting() {
        this.state = STARTING;
        for (DeployListener<E> listener : listeners) {
            try {
                listener.onStarting(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle starting event", e);
            }
        }
    }

    protected void setStarted() {
        this.state = STARTED;
        for (DeployListener<E> listener : listeners) {
            try {
                listener.onStarted(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle started event", e);
            }
        }
    }

    protected void setStopping() {
        this.state = STOPPING;
        for (DeployListener<E> listener : listeners) {
            try {
                listener.onStopping(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle stopping event", e);
            }
        }
    }

    protected void setStopped() {
        this.state = STOPPED;
        for (DeployListener<E> listener : listeners) {
            try {
                listener.onStopped(scopeModel);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle stopped event", e);
            }
        }
    }

    protected void setFailed(Throwable error) {
        this.state = FAILED;
        this.lastError = error;
        for (DeployListener<E> listener : listeners) {
            try {
                listener.onFailure(scopeModel, error);
            } catch (Throwable e) {
                logger.error(COMMON_MONITOR_EXCEPTION, "", "", getIdentifier() + " an exception occurred when handle failed event", e);
            }
        }
    }

    @Override
    public Throwable getError() {
        return lastError;
    }

    public boolean isInitialized() {
        return initialized;
    }

    protected String getIdentifier() {
        return scopeModel.getDesc();
    }
}
