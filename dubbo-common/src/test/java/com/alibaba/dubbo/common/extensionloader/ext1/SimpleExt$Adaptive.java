package com.alibaba.dubbo.common.extensionloader.ext1;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * 根据自适应产生的类
 *
 */
public class SimpleExt$Adaptive implements com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt {
    public java.lang.String bang(com.alibaba.dubbo.common.URL arg0, int arg1) {throw new UnsupportedOperationException("method public abstract java.lang.String com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt.bang(com.alibaba.dubbo.common.URL,int) of interface com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt is not adaptive method!");
    }
    public java.lang.String yell(com.alibaba.dubbo.common.URL arg0, java.lang.String arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("key1", url.getParameter("key2", "impl1"));
        if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt) name from url(" + url.toString() + ") use keys([key1, key2])");
        com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt extension = (com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt.class).getExtension(extName);
        return extension.yell(arg0, arg1);
    }
    public java.lang.String echo(com.alibaba.dubbo.common.URL arg0, java.lang.String arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("simple.ext", "impl1");
        if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt) name from url(" + url.toString() + ") use keys([simple.ext])");
        com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt extension = (com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt.class).getExtension(extName);
        return extension.echo(arg0, arg1);
    }
}