package org.apache.dubbo.remoting.exchange;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class Exchanger$Adaptive implements org.apache.dubbo.remoting.exchange.Exchanger {
public org.apache.dubbo.remoting.exchange.ExchangeClient connect(org.apache.dubbo.common.URL arg0, org.apache.dubbo.remoting.exchange.ExchangeHandler arg1) throws org.apache.dubbo.remoting.RemotingException {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = url.getParameter("exchanger", "header");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.exchange.Exchanger) name from url (" + url.toString() + ") use keys([exchanger])");
org.apache.dubbo.remoting.exchange.Exchanger extension = (org.apache.dubbo.remoting.exchange.Exchanger)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.exchange.Exchanger.class).getExtension(extName);
return extension.connect(arg0, arg1);
}
public org.apache.dubbo.remoting.exchange.ExchangeServer bind(org.apache.dubbo.common.URL arg0, org.apache.dubbo.remoting.exchange.ExchangeHandler arg1) throws org.apache.dubbo.remoting.RemotingException {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = url.getParameter("exchanger", "header");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.exchange.Exchanger) name from url (" + url.toString() + ") use keys([exchanger])");
org.apache.dubbo.remoting.exchange.Exchanger extension = (org.apache.dubbo.remoting.exchange.Exchanger)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.exchange.Exchanger.class).getExtension(extName);
return extension.bind(arg0, arg1);
}
}