package org.apache.dubbo.registry;

import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Provides a way to access ServiceInstanceMetadataUtils without importing registry-api.
 */
public interface ApplicationMetadataUpdater {

    /**
     * Refresh service metadata of an application.
     */
    void refreshMetadataAndInstance(ApplicationModel applicationModel);
}
