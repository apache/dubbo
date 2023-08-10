package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class AppStartEvent extends AbstractApplicationEvent{
    public AppStartEvent(ApplicationModel applicationModel, DeployState applicationCurrentState) {
        super(applicationModel, applicationCurrentState);
    }
}
