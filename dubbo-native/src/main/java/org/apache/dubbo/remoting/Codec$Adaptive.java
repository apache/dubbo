package org.apache.dubbo.remoting;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class Codec$Adaptive implements org.apache.dubbo.remoting.Codec {
public java.lang.Object decode(org.apache.dubbo.remoting.Channel arg0, java.io.InputStream arg1) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument == null");
if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument getUrl() == null");
org.apache.dubbo.common.URL url = arg0.getUrl();
String extName = url.getParameter("codec", "adaptive");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Codec) name from url (" + url.toString() + ") use keys([codec])");
org.apache.dubbo.remoting.Codec extension = (org.apache.dubbo.remoting.Codec)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Codec.class).getExtension(extName);
return extension.decode(arg0, arg1);
}
public void encode(org.apache.dubbo.remoting.Channel arg0, java.io.OutputStream arg1, java.lang.Object arg2) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument == null");
if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument getUrl() == null");
org.apache.dubbo.common.URL url = arg0.getUrl();
String extName = url.getParameter("codec", "adaptive");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Codec) name from url (" + url.toString() + ") use keys([codec])");
org.apache.dubbo.remoting.Codec extension = (org.apache.dubbo.remoting.Codec)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Codec.class).getExtension(extName);
extension.encode(arg0, arg1, arg2);
}
}