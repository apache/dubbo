package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class ConfiguratorFactory$Adaptive implements org.apache.dubbo.rpc.cluster.ConfiguratorFactory {
    public org.apache.dubbo.rpc.cluster.Configurator getConfigurator(org.apache.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getProtocol();
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(org.apache.dubbo.rpc.cluster.ConfiguratorFactory) name from url(" + url.toString() + ") use keys([protocol])");
        org.apache.dubbo.rpc.cluster.ConfiguratorFactory extension = (org.apache.dubbo.rpc.cluster.ConfiguratorFactory) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.cluster.ConfiguratorFactory.class).getExtension(extName);
        return extension.getConfigurator(arg0);
    }
}