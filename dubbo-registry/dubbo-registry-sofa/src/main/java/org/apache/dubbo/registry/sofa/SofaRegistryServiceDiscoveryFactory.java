package org.apache.dubbo.registry.sofa;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceDiscovery;

public class SofaRegistryServiceDiscoveryFactory extends AbstractServiceDiscoveryFactory {
    @Override
    protected ServiceDiscovery createDiscovery(URL registryURL) {
        return new SofaRegistryServiceDiscovery();
    }
}
