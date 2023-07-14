package org.apache.dubbo.rpc.cluster.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;

public class GetConfiguratorUrl implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.getConfiguratorUrl;
    }


    @Override
    public boolean attachToApplication() {
        return false;
    }

    @Override
    public Object invoke(Object... params) {

        ServiceConfig<?> config = (ServiceConfig<?>) params[0];
        URL url = (URL) params[1];

        if (config.getExtensionLoader(ConfiguratorFactory.class)
            .hasExtension(url.getProtocol())) {
            url = config.getExtensionLoader(ConfiguratorFactory.class)
                .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }
        return url;
    }
}
