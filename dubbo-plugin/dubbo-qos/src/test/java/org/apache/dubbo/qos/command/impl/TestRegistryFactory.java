package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;

public class TestRegistryFactory implements RegistryFactory {
    static Registry registry;

    @Override
    public Registry getRegistry(URL url) {
        return registry;
    }
}
