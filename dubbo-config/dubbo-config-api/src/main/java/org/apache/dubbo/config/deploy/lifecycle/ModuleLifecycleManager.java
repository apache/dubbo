package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.DefaultModuleDeployer;

import java.util.List;

/**
 * Module lifecycle manager.
 * Used in a module Lifecycle managing procedure, works like {@link ApplicationLifecycleManager}.
 */
@SPI
public interface ModuleLifecycleManager extends LifecycleManager {

    /**
     * Specifies which ModuleLifecycleManager should be called before this one
     * when {@link DefaultModuleDeployer#preDestroy()}. They are guaranteed to
     * be called before this one.
     * <br>
     * If there have some required ModuleLifecycleManager not found, or it's needInitialize() returns false,
     * this ApplicationLifecycleManager will not be called.
     * <br>
     * Note that if there are cyclic dependencies between
     * PackageLifeManagers, an exception will be thrown.
     *
     * @return ApplicationLifecycleManager names. Can be null or empty list.
     */
    default List<String> dependOnModulePreDestroy(){return null;}

    /**
     * Define what to do when {@link DefaultModuleDeployer#preDestroy()}
     */
    void onModulePreDestroy();
}
