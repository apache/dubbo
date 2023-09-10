package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConfigValidateFacade;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.rpc.model.ScopeModel;

@Activate(order = -20000)
public class ValidatorRegistryDeployListener implements ApplicationLifecycle {

    @Override
    public void initialize(ApplicationContext context) {
        ScopeModel scopeModel = context.getModel();
        ConfigValidateFacade validateFacade = new ConfigValidateFacade(scopeModel);
        scopeModel.getBeanFactory().registerBean(validateFacade);
    }

    @Override
    public boolean needInitialize() {
        return true;
    }
}
