package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * base class for remote and local implementations.
 */
abstract class BaseMetadataServiceProxyFactory implements MetadataServiceProxyFactory {
    private final Map<String, MetadataService> proxies = new HashMap<>();

    public final MetadataService getProxy(ServiceInstance serviceInstance) {
        return proxies.computeIfAbsent(serviceInstance.getServiceName() + "##" +
                ServiceInstanceMetadataUtils.getExportedServicesRevision(serviceInstance), id -> createProxy(serviceInstance));
    }

    protected abstract MetadataService createProxy(ServiceInstance serviceInstance);
}
