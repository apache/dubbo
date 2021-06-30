package org.apache.dubbo.rpc.cluster.router.state;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class StateRouterFactory$Adaptive implements org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory {
public org.apache.dubbo.rpc.cluster.router.state.StateRouter getRouter(org.apache.dubbo.common.URL arg0, org.apache.dubbo.rpc.cluster.RouterChain arg1)  {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = ( url.getProtocol() == null ? "adaptive" : url.getProtocol() );
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory) name from url (" + url.toString() + ") use keys([protocol])");
org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory extension = (org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory.class).getExtension(extName);
return extension.getRouter(arg0, arg1);
}
}