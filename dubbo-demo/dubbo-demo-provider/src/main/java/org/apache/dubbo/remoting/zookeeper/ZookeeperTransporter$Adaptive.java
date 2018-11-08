package org.apache.dubbo.remoting.zookeeper;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class ZookeeperTransporter$Adaptive implements org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter {
    public org.apache.dubbo.remoting.zookeeper.ZookeeperClient connect(org.apache.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("client", url.getParameter("transporter", "curator"));
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter) name from url(" + url.toString() + ") use keys([client, transporter])");
        org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter extension = (org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter) ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter.class).getExtension(extName);
        return extension.connect(arg0);
    }
}