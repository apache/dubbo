package org.apache.dubbo.common.threadpool;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class ThreadPool$Adaptive implements org.apache.dubbo.common.threadpool.ThreadPool {
    public java.util.concurrent.Executor getExecutor(org.apache.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("threadpool", "fixed");
        if(extName == null) throw new IllegalStateException("Fail to get extension(org.apache.dubbo.common.threadpool.ThreadPool) name from url(" + url.toString() + ") use keys([threadpool])");
        org.apache.dubbo.common.threadpool.ThreadPool extension = (org.apache.dubbo.common.threadpool.ThreadPool)ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.threadpool.ThreadPool.class).getExtension(extName);
        return extension.getExecutor(arg0);
    }
}