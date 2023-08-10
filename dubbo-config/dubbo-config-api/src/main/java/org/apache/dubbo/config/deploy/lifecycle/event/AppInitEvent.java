package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class AppInitEvent extends AbstractApplicationEvent{

    public AppInitEvent(ApplicationModel applicationModel, DeployState applicationCurrentState) {
        super(applicationModel, applicationCurrentState);
    }
}
