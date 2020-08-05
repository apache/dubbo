package org.apache.dubbo.registry.sofa;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceDiscovery;

/**
 * @author: hongwei.quhw
 * @date: 2020-08-06 14:23
 */
public class SofaRegistryServiceDiscoveryFactory extends AbstractServiceDiscoveryFactory {
    @Override
    protected ServiceDiscovery createDiscovery(URL registryURL) {
        return null;
    }
}
