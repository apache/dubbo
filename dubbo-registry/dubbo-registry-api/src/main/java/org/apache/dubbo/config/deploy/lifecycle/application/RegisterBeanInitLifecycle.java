package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.registry.DefaultMetadataUpdater;

@Activate(order = -10000)
public class RegisterBeanInitLifecycle implements ApplicationLifecycle{

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public void initialize(ApplicationContext context) {
        context.getModel().getBeanFactory().getOrRegisterBean(DefaultMetadataUpdater.class);
        System.out.println("DefaultMetadataUpdater registered.");
    }
}
