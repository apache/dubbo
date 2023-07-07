package org.apache.dubbo.config;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;

/**
 * PackageLifeCycleManager.
 * <br>
 * Used in an application Lifecycle managing procedure, and dubbo packages
 * can implement this interface to define what to do when Application start or
 * destroy.
 * <br>
 * In other word , when methods like
 * {@link DefaultApplicationDeployer#initialize()},
 * {@link DefaultApplicationDeployer#preDestroy()},
 * {@link DefaultApplicationDeployer#postDestroy()} and
 * {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)}
 *  called, all implementations of this interface will also be called.
 */
@SPI
public interface PackageLifeCycleManager {

    /**
     * Set application deployer.
     *
     * @param defaultApplicationDeployer The ApplicationDeployer that called this PackageLifeCycleManager.
     */
    void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer);


    /**
     * Returns the name of this manager.
     *
     * @return the name of this manager.
     */
    String name();

    /**
     * Whether this package need to deploy. If not, other lifecycle managing method will not be called.
     *
     * @return whether this package need to deploy
     */
    boolean needInitialize();

    /**
     * Specifies which PackageLifeCycleManager should be called before this one
     * when {@link DefaultApplicationDeployer#initialize()}. They are guaranteed to
     * be called before this one.
     * <br>
     * If there have some required PackageLifeCycleManager not found, or it's needInitialize() returns false,
     * this PackageLifeCycleManager will not be called.
     * <br>
     * Note that if there are cyclic dependencies between
     * PackageLifeManagers, an {@link IllegalStateException} will be thrown.
     *
     * @return PackageLifeCycleManager names. Can be null or empty list.
     */
    default List<String> dependOnInit() {
        return null;
    }

    /**
     * Initialize.
     */
    void initialize();

    /**
     * Specifies which PackageLifeCycleManager should be called before this one
     * when {@link DefaultApplicationDeployer#preDestroy()}.
     * <br>
     * Works just like {@link PackageLifeCycleManager#dependOnInit()}.
     *
     * @return PackageLifeCycleManager names. Can be null or empty list.
     */
    default List<String> dependOnPreDestroy() {
        return null;
    }

    /**
     * preDestroy.
     */
    default void preDestroy() {}

    /**
     * Specifies which PackageLifeCycleManager should be called before this one
     * when {@link DefaultApplicationDeployer#postDestroy()}.
     * <br>
     * Works just like {@link PackageLifeCycleManager#dependOnInit()}.
     *
     * @return PackageLifeCycleManager names. Can be null or empty list.
     */
    default List<String> dependOnPostDestroy() {
        return null;
    }

    /**
     * postDestroy.
     */
    default void postDestroy() {}

    /**
     * What to do when a module changed.
     *
     * @param changedModule changedModule
     * @param moduleState moduleState
     */
    default void moduleChanged(ModuleModel changedModule,DeployState moduleState){}

    /**
     * Specifies which PackageLifeCycleManager should be called before this one when {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)}. Works just like dependOnInit().
     *
     * @return PackageLifeCycleManager names. Can be null or empty list.
     */
    default List<String> dependOnModuleChanged(){return null;}

}
