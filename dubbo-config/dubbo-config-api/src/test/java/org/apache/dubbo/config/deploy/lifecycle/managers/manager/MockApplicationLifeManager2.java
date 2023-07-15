package org.apache.dubbo.config.deploy.lifecycle.managers.manager;

import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycleManager;

public class MockApplicationLifeManager2 implements ApplicationLifecycleManager {


    private static final String NAME = "manager2";


    @Override
    public void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer) {

    }

    @Override
    public void initialize() {

    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean needInitialize() {
        return true;
    }
}
