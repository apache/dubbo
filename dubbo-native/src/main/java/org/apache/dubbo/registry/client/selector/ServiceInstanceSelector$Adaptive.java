package org.apache.dubbo.registry.client.selector;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class ServiceInstanceSelector$Adaptive implements org.apache.dubbo.registry.client.selector.ServiceInstanceSelector {
public org.apache.dubbo.registry.client.ServiceInstance select(org.apache.dubbo.common.URL arg0, java.util.List arg1)  {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = url.getParameter("service-instance-selector", "random");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.registry.client.selector.ServiceInstanceSelector) name from url (" + url.toString() + ") use keys([service-instance-selector])");
org.apache.dubbo.registry.client.selector.ServiceInstanceSelector extension = (org.apache.dubbo.registry.client.selector.ServiceInstanceSelector)ExtensionLoader.getExtensionLoader(org.apache.dubbo.registry.client.selector.ServiceInstanceSelector.class).getExtension(extName);
return extension.select(arg0, arg1);
}
}