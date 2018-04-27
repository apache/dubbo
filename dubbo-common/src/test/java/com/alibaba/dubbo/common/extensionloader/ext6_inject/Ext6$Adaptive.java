package com.alibaba.dubbo.common.extensionloader.ext6_inject;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class Ext6$Adaptive implements com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6 {
    public java.lang.String echo(com.alibaba.dubbo.common.URL arg0, java.lang.String arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("ext6");
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6) name from url(" + url.toString() + ") use keys([ext6])");
        com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6 extension = (com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6.class).getExtension(extName);
        return extension.echo(arg0, arg1);
    }
}