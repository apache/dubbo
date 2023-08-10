package org.apache.dubbo.config.deploy.lifecycle.context;

import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractModelContext<T extends ScopeModel> implements ModelContext<T> {

    private final T scopeModel;

    private final List<DeployListener<T>> listeners;

    private DeployState modelState;

    private boolean initialized;

    private Throwable lastError;

    public AbstractModelContext(T scopeModel) {
        this.modelState = DeployState.PENDING;
        this.scopeModel = scopeModel;
        this.listeners = new CopyOnWriteArrayList<>();
        this.initialized = false;
    }

    @Override
    public T getModel() {
        return scopeModel;
    }

    @Override
    public void addDeployListener(DeployListener<T> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeDeployListener(DeployListener<T> listener) {
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
    public List<DeployListener<T>> getListeners() {
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
