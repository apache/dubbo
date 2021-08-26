package org.apache.dubbo.common.beans.factory;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelPostProcessor;

/**
 * Initialize the bean factory for ScopeModel
 */
public abstract class ScopeBeanFactoryInitializer implements ScopeModelPostProcessor {

    @Override
    public void postProcessScopeModel(ScopeModel scopeModel) {
        if (scopeModel instanceof ApplicationModel) {
            ApplicationModel applicationModel = (ApplicationModel) scopeModel;
            registerApplicationBeans(applicationModel, applicationModel.getBeanFactory());
        } else if (scopeModel instanceof FrameworkModel) {
            FrameworkModel frameworkModel = (FrameworkModel) scopeModel;
            registerFrameworkBeans(frameworkModel, frameworkModel.getBeanFactory());
        } else if (scopeModel instanceof ModuleModel) {
            ModuleModel moduleModel = (ModuleModel) scopeModel;
            registerModuleBeans(moduleModel, moduleModel.getBeanFactory());
        }
    }

    /**
     * Initialize beans for framework
     *
     * @param frameworkModel
     * @param beanFactory
     */
    protected void registerFrameworkBeans(FrameworkModel frameworkModel, ScopeBeanFactory beanFactory) {

    }

    /**
     * Initialize beans for application
     *
     * @param applicationModel
     * @param beanFactory
     */
    protected void registerApplicationBeans(ApplicationModel applicationModel, ScopeBeanFactory beanFactory) {
//        beanFactory.registerBean(MetadataReportInstance.class);
//        beanFactory.registerBean(RemoteMetadataServiceImpl.class);
    }

    /**
     * Initialize beans for module
     *
     * @param moduleModel
     * @param beanFactory
     */
    protected void registerModuleBeans(ModuleModel moduleModel, ScopeBeanFactory beanFactory) {

    }

}
