package org.apache.dubbo.config.deploy.lifecycle;

/**
 * Lifecycle.
 */
public interface Lifecycle {

    /**
     * The name of this Lifecycle.
     *
     * @return the name of this manager.
     */
    String name();

    /**
     * If this lifecycle need to initialize.
     */
    boolean needInitialize();
}
