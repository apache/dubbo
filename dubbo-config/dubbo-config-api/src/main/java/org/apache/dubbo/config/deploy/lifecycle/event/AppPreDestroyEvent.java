package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppPreDestroyEvent extends AbstractApplicationEvent{

   private final AtomicBoolean registered;

    public AppPreDestroyEvent(ApplicationModel applicationModel, DeployState applicationCurrentState,AtomicBoolean registered) {
        super(applicationModel,applicationCurrentState);
        this.registered = registered;
    }

    public AtomicBoolean registered() {
        return registered;
    }
}
