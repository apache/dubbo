package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

/**
 * Metadata publisher.
 */
@SPI
public interface MetadataPublisher {

    /**
     * Publish a service metadata to all available MetadataReport.
     */
    void publishServiceDefinition(URL url, ServiceDescriptor serviceDescriptor, ApplicationModel applicationModel);
}
