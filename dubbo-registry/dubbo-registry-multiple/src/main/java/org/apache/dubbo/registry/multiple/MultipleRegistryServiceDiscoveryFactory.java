package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceDiscovery;

public class MultipleRegistryServiceDiscoveryFactory extends AbstractServiceDiscoveryFactory  {
    @Override
    protected ServiceDiscovery createDiscovery(URL registryURL) {
        return new MultipleRegistryServiceDiscovery();
    }
}
