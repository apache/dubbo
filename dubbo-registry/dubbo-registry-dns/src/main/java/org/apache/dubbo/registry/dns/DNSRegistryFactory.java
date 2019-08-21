package org.apache.dubbo.registry.dns;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

public class DNSRegistryFactory extends AbstractRegistryFactory {
    @Override
    protected Registry createRegistry(URL url) {
        return new DNSRegistry(url);
    }
}
