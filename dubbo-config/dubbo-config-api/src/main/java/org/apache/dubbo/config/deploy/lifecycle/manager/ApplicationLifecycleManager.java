package org.apache.dubbo.config.deploy.lifecycle.manager;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle;
import org.apache.dubbo.config.deploy.lifecycle.loader.LifecycleManager;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application Life Manager Loader
 */
public class ApplicationLifecycleManager extends LifecycleManager<ApplicationLifecycle> {

//    private static final String INIT = "init";
//
//    private static final String PRE_DESTROY = "preDestroy";
//
//    private static final String POST_DESTROY = "postDestroy";
//
//    private static final String PRE_MODULE_CHANGED = "preModuleChanged";
//
//    private static final String POST_MODULE_CHANGED = "postModuleChanged";
//
//    private static final String REFRESH_SERVICE_INSTANCE = "refreshServiceInstance";

    private final DefaultApplicationDeployer defaultApplicationDeployer;


    public ApplicationLifecycleManager(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.defaultApplicationDeployer = defaultApplicationDeployer;
    }

    public void start(AtomicBoolean hasPreparedApplicationInstance){
        getAll().forEach(applicationLifecycle -> applicationLifecycle.start(hasPreparedApplicationInstance));
//        getAll().forEach((name, applicationLifecycle) -> applicationLifecycle.start(hasPreparedApplicationInstance));
    }

    public void initialize() {
        getAll().forEach(ApplicationLifecycle::initialize);
//        getSequenceByOperationName(INIT).forEach(ApplicationLifecycle::initialize);
    }

    public void preDestroy() {
        getAll().forEach(ApplicationLifecycle::preDestroy);
//        getSequenceByOperationName(PRE_DESTROY).forEach(ApplicationLifecycle::preDestroy);
    }

    public void postDestroy() {
        getAll().forEach(ApplicationLifecycle::postDestroy);
//        getSequenceByOperationName(POST_DESTROY).forEach(ApplicationLifecycle::postDestroy);
    }

    public void preModuleChanged(ModuleModel changedModule, DeployState changedModuleState, AtomicBoolean hasPreparedApplicationInstance) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.preModuleChanged(changedModule, changedModuleState, hasPreparedApplicationInstance));
//        getSequenceByOperationName(PRE_MODULE_CHANGED).forEach(packageLifeManager -> packageLifeManager.preModuleChanged(changedModule, changedModuleState,hasPreparedApplicationInstance));
    }

    public void postModuleChanged(ModuleModel changedModule, DeployState changedModuleState, DeployState applicationNewState) {
        getAll().forEach(applicationLifecycle -> applicationLifecycle.postModuleChanged(changedModule, changedModuleState, applicationNewState));
//        getSequenceByOperationName(POST_MODULE_CHANGED).forEach(packageLifeCycleManager -> packageLifeCycleManager.postModuleChanged(changedModule, changedModuleState, applicationNewState));
    }

    public void runRefreshServiceInstance(){
        getAll().forEach(ApplicationLifecycle::refreshServiceInstance);
//        getSequenceByOperationName(REFRESH_SERVICE_INSTANCE).forEach(ApplicationLifecycle::refreshServiceInstance);
    }


//    /**
//     * Map method name to the method that provides dependency relations.
//     */
//    @Override
//    protected void mapOperationsToDependencyProvider(Map<String, Function<ApplicationLifecycle, List<String>>> dependencyProviders) {
//        dependencyProviders.put(INIT, ApplicationLifecycle::dependOnInit);
//        dependencyProviders.put(PRE_DESTROY, ApplicationLifecycle::dependOnPreDestroy);
//        dependencyProviders.put(POST_DESTROY, ApplicationLifecycle::postDestroyDependencies);
//        dependencyProviders.put(PRE_MODULE_CHANGED, ApplicationLifecycle::dependOnPreModuleChanged);
//        dependencyProviders.put(POST_MODULE_CHANGED, ApplicationLifecycle::dependOnPostModuleChanged);
//        dependencyProviders.put(REFRESH_SERVICE_INSTANCE, ApplicationLifecycle::dependOnRefreshServiceInstance);
//    }

    @Override
    protected List<ApplicationLifecycle> loadAll() {
        ExtensionLoader<ApplicationLifecycle> loader = defaultApplicationDeployer.getApplicationModel().getExtensionLoader(ApplicationLifecycle.class);
        return loader.getActivateExtensions();
    }
}
