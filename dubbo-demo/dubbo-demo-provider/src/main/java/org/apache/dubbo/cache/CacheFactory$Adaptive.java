package org.apache.dubbo.cache;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class CacheFactory$Adaptive implements org.apache.dubbo.cache.CacheFactory {
    public org.apache.dubbo.cache.Cache getCache(org.apache.dubbo.common.URL arg0, org.apache.dubbo.rpc.Invocation arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("cache", "lru");
        if(extName == null) throw new IllegalStateException("Fail to get extension(org.apache.dubbo.cache.CacheFactory) name from url(" + url.toString() + ") use keys([cache])");
        org.apache.dubbo.cache.CacheFactory extension = (org.apache.dubbo.cache.CacheFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.cache.CacheFactory.class).getExtension(extName);
        return extension.getCache(arg0, arg1);
    }
}
