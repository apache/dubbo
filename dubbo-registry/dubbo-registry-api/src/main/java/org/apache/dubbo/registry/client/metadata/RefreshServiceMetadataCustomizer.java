package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;

import static org.apache.dubbo.metadata.WritableMetadataService.DEFAULT_EXTENSION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataStorageType;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getSubscribedServicesRevision;

/**
 * An {@link ServiceInstanceCustomizer} to refresh metadata.
 * <p>
 * 2019-08-08
 */
public class RefreshServiceMetadataCustomizer implements ServiceInstanceCustomizer {

    public int getPriority() {
        return MIN_PRIORITY;
    }

    @Override
    public void customize(ServiceInstance serviceInstance) {
        // FIXME to define the constant
        String metadataStoredType = getMetadataStorageType(serviceInstance);
        WritableMetadataService remoteWritableMetadataService =
                WritableMetadataService.getExtension(metadataStoredType == null ? DEFAULT_EXTENSION : metadataStoredType);

        remoteWritableMetadataService.refreshMetadata(getExportedServicesRevision(serviceInstance),
                getSubscribedServicesRevision(serviceInstance));
    }
}
