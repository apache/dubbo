package org.apache.dubbo.config.deploy.lifecycle.manager;

import org.apache.dubbo.config.deploy.DefaultModuleDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ModuleLifecycleManager;

/**
 * Default Module Life Cycle Manager.
 */
public class DefaultModuleLifeManager implements ModuleLifecycleManager {

    private static final String NAME = "default";

    private DefaultModuleDeployer moduleDeployer;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean needInitialize() {
        return true;
    }

    public void setDeployer(DefaultModuleDeployer moduleDeployer) {
        this.moduleDeployer = moduleDeployer;
    }

    @Override
    public void onModulePreDestroy() {
        moduleDeployer.onModuleStopping();
    }
}
