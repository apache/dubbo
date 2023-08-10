package org.apache.dubbo.config.deploy.lifecycle.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

public abstract class AbstractApplicationEvent implements ApplicationEvent {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(this.getClass());

    private final ApplicationModel applicationModel;

    private DeployState applicationCurrentState;

    public AbstractApplicationEvent(ApplicationModel applicationModel,DeployState applicationCurrentState) {
        this.applicationModel = applicationModel;
        this.applicationCurrentState = applicationCurrentState;
    }

    @Override
    public ErrorTypeAwareLogger getLogger() {
        return logger;
    }

    @Override
    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    @Override
    public DeployState applicationCurrentState() {
        return applicationCurrentState;
    }
}
