package org.apache.dubbo.config.deploy.lifecycle;

/**
 * Lifecycle Manager.
 */
public interface Lifecycle {

    /**
     * The name of this manager.
     *
     * @return the name of this manager.
     */
    String name();

    /**
     * If this lifecycle manager need to initialize.
     */
    boolean needInitialize();
}
