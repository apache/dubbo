package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppPostModuleChangeEvent extends AbstractApplicationEvent{

    private final ModuleModel changedModule;

    private final DeployState moduleNewState;

    private final DeployState applicationOldState;

    private final DeployState applicationNewState;

    private AtomicBoolean registered;

    public AppPostModuleChangeEvent(ApplicationModel applicationModel,ModuleModel changedModule,DeployState moduleNewState, DeployState applicationNewState, DeployState applicationOldState,AtomicBoolean registered) {
        super(applicationModel,applicationNewState);
        this.changedModule = changedModule;
        this.moduleNewState = moduleNewState;
        this.applicationNewState = applicationNewState;
        this.applicationOldState = applicationOldState;
        this.registered =registered;
    }

    public ModuleModel getChangedModule() {
        return changedModule;
    }

    public DeployState getModuleNewState() {
        return moduleNewState;
    }

    public DeployState getApplicationOldState() {
        return applicationOldState;
    }

    public DeployState getApplicationNewState() {
        return applicationNewState;
    }

    public AtomicBoolean registered() {
        return registered;
    }
}
