package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;

/**
 * 2019-08-09
 */
abstract class BaseMetadataServiceProxyFactory implements MetadataServiceProxyFactory {
    private final Map<String, MetadataService> proxies = new HashMap<>();

    public final MetadataService getProxy(ServiceInstance serviceInstance) {
        return proxies.computeIfAbsent(serviceInstance.getServiceName() + "##" +
                serviceInstance.getMetadata().getOrDefault(REVISION_KEY, ""), id -> createProxy(serviceInstance));
    }

    protected abstract MetadataService createProxy(ServiceInstance serviceInstance);
}
