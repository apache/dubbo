package org.apache.dubbo.configcenter.consul;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.configcenter.AbstractDynamicConfigurationFactory;
import org.apache.dubbo.configcenter.DynamicConfiguration;

public class ConsulDynamicConfigurationFactory extends AbstractDynamicConfigurationFactory {
    @Override
    protected DynamicConfiguration createDynamicConfiguration(URL url) {
        return new ConsulDynamicConfiguration(url);
    }
}
