package org.apache.dubbo.config.deploy.lifecycle.loader;


import org.apache.dubbo.config.deploy.DefaultModuleDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ModuleLifecycleManager;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModuleLifecycleManagerLoader extends AbstractLifecycleManagerLoader<ModuleLifecycleManager> {

    private final DefaultModuleDeployer moduleDeployer;

    private static final String PRE_DESTROY = "preDestroy";

    public ModuleLifecycleManagerLoader(DefaultModuleDeployer moduleDeployer) {
        this.moduleDeployer = moduleDeployer;
    }

    public void moduleRunPreDestroy(){
        this.getSequenceByOperationName(PRE_DESTROY).forEach(ModuleLifecycleManager::onModulePreDestroy);
    }

    @Override
    protected List<ModuleLifecycleManager> loadManagers() {
        return moduleDeployer.getModuleModel().getBeanFactory().getBeansOfType(ModuleLifecycleManager.class);
    }

    /**
     * Map method name to the method that provides dependency relations.
     */
    @Override
    protected void mapOperationsToDependencyProvider(Map<String, Function<ModuleLifecycleManager, List<String>>> dependencyProviders) {
        dependencyProviders.put(PRE_DESTROY,ModuleLifecycleManager::dependOnModulePreDestroy);
    }


}
