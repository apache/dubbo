package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppServiceRefreshEvent extends AbstractApplicationEvent {

    private final AtomicBoolean registered;

    public AppServiceRefreshEvent(ApplicationModel applicationModel, DeployState applicationCurrentState,AtomicBoolean registered) {
        super(applicationModel,applicationCurrentState);
        this.registered = registered;
    }

    public AtomicBoolean getRegistered() {
        return registered;
    }
}
