package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.rpc.model.ModuleModel;

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
    default void start(AtomicBoolean hasPreparedApplicationInstance){
        return;
    }

    /**
     * {@link  ApplicationDeployer#initialize()}
     */
    default void initialize(){};

    /**
     * {@link ApplicationDeployer#preDestroy()}
     */
    default void preDestroy() {}

    /**
     * {@link ApplicationDeployer#postDestroy()}
     */
    default void postDestroy() {}

    /**
     * What to do when a module changed.
     *
     * @param changedModule changed module
     * @param moduleState module state
     */
    default void preModuleChanged(ModuleModel changedModule, DeployState moduleState, AtomicBoolean hasPreparedApplicationInstance){}

    /**
     * What to do after a module changed.
     * @param changedModule changed module
     * @param moduleState module state
     * @param newState new application state
     */
    default void postModuleChanged(ModuleModel changedModule,DeployState moduleState, DeployState newState){}

    /**
     * {@link DefaultApplicationDeployer#refreshServiceInstance()}.
     */
    default void refreshServiceInstance(){return;}

}
