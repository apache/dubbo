package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;

/**
 * ApplicationLifecycleManager.
 * <br>
 * Used in an application Lifecycle managing procedure, and dubbo packages
 * can implement this interface to define what to do when Application start or
 * destroy.
 * <br>
 * In other word , when methods like
 * {@link DefaultApplicationDeployer#initialize()},
 * {@link DefaultApplicationDeployer#preDestroy()},
 * {@link DefaultApplicationDeployer#postDestroy()} and
 * {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)} etc.
 *  called, all implementations of this interface will also be called.
 */
@SPI
public interface ApplicationLifecycleManager extends LifecycleManager {

    /**
     * Set application deployer.
     *
     * @param defaultApplicationDeployer The ApplicationDeployer that called this ApplicationLifecycleManager.
     */
    void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer);

    /**
     * Specifies which ApplicationLifecycleManager should be called before this one
     * when {@link DefaultApplicationDeployer#initialize()}. They are guaranteed to
     * be called before this one.
     * <br>
     * If there have some required ApplicationLifecycleManager not found, or it's needInitialize() returns false,
     * this ApplicationLifecycleManager will not be called.
     * <br>
     * Note that if there are cyclic dependencies between
     * PackageLifeManagers, an {@link IllegalStateException} will be thrown.
     *
     * @return ApplicationLifecycleManager names. Can be null or empty list.
     */
    default List<String> dependOnInit() {
        return null;
    }

    /**
     * Initialize.
     */
    void initialize();

    /**
     * Specifies which ApplicationLifecycleManager should be called before this one
     * when {@link DefaultApplicationDeployer#preDestroy()}.
     * <br>
     * Works just like {@link ApplicationLifecycleManager#dependOnInit()}.
     *
     * @return ApplicationLifecycleManager names. Can be null or empty list.
     */
    default List<String> dependOnPreDestroy() {
        return null;
    }

    /**
     * preDestroy.
     */
    default void preDestroy() {}

    /**
     * Specifies which ApplicationLifecycleManager should be called before this one
     * when {@link DefaultApplicationDeployer#postDestroy()}.
     * <br>
     * Works just like {@link ApplicationLifecycleManager#dependOnInit()}.
     *
     * @return ApplicationLifecycleManager names. Can be null or empty list.
     */
    default List<String> postDestroyDependencies() {
        return null;
    }

    /**
     * postDestroy.
     */
    default void postDestroy() {}

    /**
     * What to do when a module changed.
     *
     * @param changedModule changed module
     * @param moduleState module state
     */
    default void preModuleChanged(ModuleModel changedModule, DeployState moduleState){}

    /**
     * Specifies which ApplicationLifecycleManager should be called before this one when {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)}. Works just like dependOnInit().
     *
     * @return ApplicationLifecycleManager names. Can be null or empty list.
     */
    default List<String> dependOnPreModuleChanged(){return null;}

    /**
     * What to do after a module changed.
     * @param changedModule changed module
     * @param moduleState module state
     * @param newState new application state
     */
    default void postModuleChanged(ModuleModel changedModule,DeployState moduleState, DeployState newState){}

    /**
     * Specifies which ApplicationLifecycleManager should be called before this one after {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)}. Works just like dependOnInit().
     *
     * @return ApplicationLifecycleManager names. Can be null or empty list.
     */
    default List<String> dependOnPostModuleChanged(){return null;}

    /**
     * What to do when {@link DefaultApplicationDeployer#refreshServiceInstance()}.
     */
    default void onRefreshServiceInstance(){return;}

    /**
     * Specifies which ApplicationLifecycleManager should be called before this one after {@link DefaultApplicationDeployer#refreshServiceInstance()}. Works just like dependOnInit().
     *
     * @return ApplicationLifecycleManager names. Can be null or empty list.
     */
    default List<String> dependOnRefreshServiceInstance(){return null;}

}
