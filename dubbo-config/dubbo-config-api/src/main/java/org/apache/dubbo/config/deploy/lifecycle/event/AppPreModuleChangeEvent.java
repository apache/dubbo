package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
