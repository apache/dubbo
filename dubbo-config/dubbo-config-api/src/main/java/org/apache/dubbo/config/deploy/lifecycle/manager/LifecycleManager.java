package org.apache.dubbo.config.deploy.lifecycle.manager;

/**
 * Manager of {@link  org.apache.dubbo.config.deploy.lifecycle.Lifecycle} implements
 */
public interface LifecycleManager{

    void start();

    void initialize();

    void preDestroy();

    void postDestroy();
}
