package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

public class CertScopeModelInitializer implements ScopeModelInitializer {
    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        beanFactory.registerBean(DubboCertManager.class);
    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {

    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {

    }
}
