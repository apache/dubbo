package org.apache.dubbo.config.deploy.lifecycle.loader;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycleManager;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Application Life Manager Loader
 */
public class ApplicationLifeManagerLoader extends AbstractLifecycleManagerLoader<ApplicationLifecycleManager> {

    private static final String INIT = "init";

    private static final String PRE_DESTROY = "preDestroy";

    private static final String POST_DESTROY = "postDestroy";

    private static final String PRE_MODULE_CHANGED = "preModuleChanged";

    private static final String POST_MODULE_CHANGED = "postModuleChanged";

    private static final String REFRESH_SERVICE_INSTANCE = "refreshServiceInstance";

    private final DefaultApplicationDeployer defaultApplicationDeployer;


    public ApplicationLifeManagerLoader(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.defaultApplicationDeployer = defaultApplicationDeployer;
    }

    public void packageRunInit() {
        getSequenceByOperationName(INIT).forEach(ApplicationLifecycleManager::initialize);
    }

    public void appRunPreDestroy() {
        getSequenceByOperationName(PRE_DESTROY).forEach(ApplicationLifecycleManager::preDestroy);
    }

    public void appRunPostDestroy() {
        getSequenceByOperationName(POST_DESTROY).forEach(ApplicationLifecycleManager::postDestroy);
    }

    public void appRunPreModuleChanged(ModuleModel changedModule, DeployState changedModuleState) {
        getSequenceByOperationName(PRE_MODULE_CHANGED).forEach(packageLifeManager -> packageLifeManager.preModuleChanged(changedModule, changedModuleState));
    }

    public void appRunPostModuleChanged(ModuleModel changedModule, DeployState changedModuleState, DeployState applicationNewState) {
        getSequenceByOperationName(POST_MODULE_CHANGED).forEach(packageLifeCycleManager -> packageLifeCycleManager.postModuleChanged(changedModule, changedModuleState, applicationNewState));
    }

    public void appRunRefreshServiceInstance(){
        getSequenceByOperationName(REFRESH_SERVICE_INSTANCE).forEach(ApplicationLifecycleManager::onRefreshServiceInstance);
    }


    /**
     * Map operation name to the method that provides dependency relations.
     */
    @Override
    protected void mapOperationsToDependencyProvider(Map<String, Function<ApplicationLifecycleManager, List<String>>> dependencyProviders) {
        dependencyProviders.put(INIT,ApplicationLifecycleManager::dependOnInit);
        dependencyProviders.put(PRE_DESTROY,ApplicationLifecycleManager::dependOnPreDestroy);
        dependencyProviders.put(POST_DESTROY,ApplicationLifecycleManager::postDestroyDependencies);
        dependencyProviders.put(PRE_MODULE_CHANGED,ApplicationLifecycleManager::dependOnPreModuleChanged);
        dependencyProviders.put(POST_MODULE_CHANGED,ApplicationLifecycleManager::dependOnPostModuleChanged);
        dependencyProviders.put(REFRESH_SERVICE_INSTANCE,ApplicationLifecycleManager::dependOnRefreshServiceInstance);
    }

    @Override
    protected List<ApplicationLifecycleManager> loadManagers() {
        ExtensionLoader<ApplicationLifecycleManager> loader = defaultApplicationDeployer.getExtensionLoader(ApplicationLifecycleManager.class);
        return loader.getActivateExtensions();
    }
}
