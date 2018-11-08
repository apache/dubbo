package org.apache.dubbo.registry;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class RegistryFactory$Adaptive implements org.apache.dubbo.registry.RegistryFactory {
    public org.apache.dubbo.registry.Registry getRegistry(org.apache.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );
        if(extName == null) throw new IllegalStateException("Fail to get extension(org.apache.dubbo.registry.RegistryFactory) name from url(" + url.toString() + ") use keys([protocol])");
        org.apache.dubbo.registry.RegistryFactory extension = (org.apache.dubbo.registry.RegistryFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.registry.RegistryFactory.class).getExtension(extName);
        return extension.getRegistry(arg0);
    }
}