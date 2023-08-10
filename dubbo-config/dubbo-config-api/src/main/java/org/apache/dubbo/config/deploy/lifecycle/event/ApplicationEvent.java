package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.rpc.model.ApplicationModel;

public interface ApplicationEvent {

    ErrorTypeAwareLogger getLogger();

    ApplicationModel getApplicationModel();

    DeployState applicationCurrentState();
}
