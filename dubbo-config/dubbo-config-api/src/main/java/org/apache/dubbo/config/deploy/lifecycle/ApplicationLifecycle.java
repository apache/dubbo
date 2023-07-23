package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ApplicationLifecycle.
 * <br>
 * Used in an application Lifecycle managing procedure, and dubbo packages
 * can implement this interface to define what to do when application status changes.
 * <br>
 * In another word, when methods like
 * {@link DefaultApplicationDeployer#start()},
 * {@link DefaultApplicationDeployer#initialize()},
 * {@link DefaultApplicationDeployer#preDestroy()},
 * {@link DefaultApplicationDeployer#postDestroy()} and
 * {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)} etc.
 *  called, all implementations of this interface will also be called.
 */
@SPI
public interface ApplicationLifecycle extends Lifecycle {

    /**
     * Set application deployer.
     *
     * @param defaultApplicationDeployer The ApplicationDeployer that called this ApplicationLifecycle.
     */
    void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer);

    /**
     * {@link ApplicationDeployer#start()}
     */
    default void start(){
        return;
    }

    /**
     * Specifies which ApplicationLifecycle should be called before this one
     * when {@link DefaultApplicationDeployer#initialize()}. They are guaranteed to
     * be called before this one.
     * <br>
     * If there have some required ApplicationLifecycle not found, or it's needInitialize() returns false,
     * this ApplicationLifecycle will not be called.
     * <br>
     * Note that if there are cyclic dependencies between
     * PackageLifeManagers, an exception will be thrown.
     *
     * @return ApplicationLifecycle names. Can be null or empty list.
     */
    default List<String> dependOnInit() {
        return null;
    }

    /**
     * {@link  ApplicationDeployer#initialize()}
     */
    default void initialize(){};

    /**
     * Specifies which ApplicationLifecycle should be called before this one
     * when {@link DefaultApplicationDeployer#preDestroy()}.
     * <br>
     * Works just like {@link ApplicationLifecycle#dependOnInit()}.
     *
     * @return ApplicationLifecycle names. Can be null or empty list.
     */
    default List<String> dependOnPreDestroy() {
        return null;
    }

    /**
     * {@link ApplicationDeployer#preDestroy()}
     */
    default void preDestroy() {}


    /**
     * Specifies which ApplicationLifecycle should be called before this one
     * when {@link DefaultApplicationDeployer#postDestroy()}.
     * <br>
     * Works just like {@link ApplicationLifecycle#dependOnInit()}.
     *
     * @return ApplicationLifecycle names. Can be null or empty list.
     */
    default List<String> postDestroyDependencies() {
        return null;
    }

    /**
     * {@link ApplicationDeployer#postDestroy()}
     */
    default void postDestroy() {}


    /**
     * Specifies which ApplicationLifecycle should be called before this one when {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)}. Works just like {@link ApplicationLifecycle#dependOnInit()}.
     *
     * @return ApplicationLifecycle names. Can be null or empty list.
     */
    default List<String> dependOnPreModuleChanged(){return null;}

    /**
     * What to do when a module changed.
     *
     * @param changedModule changed module
     * @param moduleState module state
     */
    default void preModuleChanged(ModuleModel changedModule, DeployState moduleState, AtomicBoolean hasPreparedApplicationInstance){}


    /**
     * Specifies which ApplicationLifecycle should be called before this one after {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)}. Works just like {@link ApplicationLifecycle#dependOnInit()}.
     *
     * @return ApplicationLifecycle names. Can be null or empty list.
     */
    default List<String> dependOnPostModuleChanged(){return null;}

    /**
     * What to do after a module changed.
     * @param changedModule changed module
     * @param moduleState module state
     * @param newState new application state
     */
    default void postModuleChanged(ModuleModel changedModule,DeployState moduleState, DeployState newState){}


    /**
     * Specifies which ApplicationLifecycle should be called before this one after {@link DefaultApplicationDeployer#refreshServiceInstance()}. Works just like {@link ApplicationLifecycle#dependOnInit()}.
     *
     * @return ApplicationLifecycle names. Can be null or empty list.
     */
    default List<String> dependOnRefreshServiceInstance(){return null;}

    /**
     * {@link DefaultApplicationDeployer#refreshServiceInstance()}.
     */
    default void refreshServiceInstance(){return;}

}
