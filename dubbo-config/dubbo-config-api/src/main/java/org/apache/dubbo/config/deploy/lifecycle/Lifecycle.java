package org.apache.dubbo.config.deploy.lifecycle;

/**
 * Lifecycle.
 */
public interface Lifecycle {

    /**
     * If this lifecycle need to initialize.
     */
    boolean needInitialize();
}
