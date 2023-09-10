package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.config.ConfigValidateFacade;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class ValidatorRegistryDeployListener implements ApplicationDeployListener {

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        ConfigValidateFacade validateFacade = new ConfigValidateFacade(scopeModel);
        scopeModel.getBeanFactory().registerBean(validateFacade);
    }

    @Override
    public void onInitialize(ApplicationModel scopeModel) {}
    @Override
    public void onStarted(ApplicationModel scopeModel) {}
    @Override
    public void onStopping(ApplicationModel scopeModel) {}
    @Override
    public void onStopped(ApplicationModel scopeModel) {}
    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {}
}
