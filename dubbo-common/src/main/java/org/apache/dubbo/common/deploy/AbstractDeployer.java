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

import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.deploy.DeployState.FAILED;
import static org.apache.dubbo.common.deploy.DeployState.PENDING;
import static org.apache.dubbo.common.deploy.DeployState.STARTED;
import static org.apache.dubbo.common.deploy.DeployState.STARTING;
import static org.apache.dubbo.common.deploy.DeployState.STOPPED;
import static org.apache.dubbo.common.deploy.DeployState.STOPPING;

public abstract class AbstractDeployer<E extends ScopeModel> implements Deployer<E> {

    private volatile DeployState state = PENDING;

    protected AtomicBoolean initialized = new AtomicBoolean(false);

    private List<DeployListener<E>> listeners = new ArrayList<>();

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
            listener.onStarting(scopeModel);
        }
    }

    protected void setStarted() {
        this.state = STARTED;
        for (DeployListener<E> listener : listeners) {
            listener.onStarted(scopeModel);
        }
    }
    protected void setStopping() {
        this.state = STOPPING;
        for (DeployListener<E> listener : listeners) {
            listener.onStopping(scopeModel);
        }
    }

    protected void setStopped() {
        this.state = STOPPED;
        for (DeployListener<E> listener : listeners) {
            listener.onStopped(scopeModel);
        }
    }

    protected void setFailed(Throwable cause) {
        this.state = FAILED;
        for (DeployListener<E> listener : listeners) {
            listener.onFailure(scopeModel, cause);
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }
}
