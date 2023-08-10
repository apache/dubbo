package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class AppPostDestroyEvent extends AbstractApplicationEvent{

    public AppPostDestroyEvent(ApplicationModel applicationModel, DeployState currentApplicationState) {
        super(applicationModel, currentApplicationState);
    }
}
