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

import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractModelContext<M extends ScopeModel> implements ModelContext<M> {

    private final M scopeModel;

    private final List<DeployListener<M>> listeners;

    private DeployState modelState;

    private boolean initialized;

    private Throwable lastError;

    public AbstractModelContext(M scopeModel) {
        this.modelState = DeployState.PENDING;
        this.scopeModel = scopeModel;
        this.listeners = new CopyOnWriteArrayList<>();
        this.initialized = false;
    }

    @Override
    public M getModel() {
        return scopeModel;
    }

    @Override
    public void addDeployListener(DeployListener<M> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeDeployListener(DeployListener<M> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void setModelState(DeployState newState) {
        this.modelState = newState;
    }

    @Override
    public DeployState getCurrentState() {
        return this.modelState;
    }

    @Override
    public List<DeployListener<M>> getListeners() {
        return listeners;
    }

    @Override
    public Throwable getLastError() {
        return lastError;
    }

    @Override
    public void setLastError(Throwable lastError) {
        this.lastError = lastError;
    }

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
