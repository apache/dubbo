package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceDiscovery;

/**
 * @author: hongwei.quhw
 * @date: 2020-09-25 15:48
 */
public class MultipleRegistryServiceDiscoveryFactory extends AbstractServiceDiscoveryFactory  {
    @Override
    protected ServiceDiscovery createDiscovery(URL registryURL) {
        return new MultipleRegistryServiceDiscovery();
    }
}
