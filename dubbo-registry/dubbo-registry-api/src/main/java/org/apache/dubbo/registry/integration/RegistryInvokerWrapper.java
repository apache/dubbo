package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;

class RegistryInvokerWrapper<T> implements Invoker<T> {
    private RegistryDirectory<T> directory;
    private Cluster cluster;
    private Invoker<T> invoker;
    private URL url;

    public RegistryInvokerWrapper(RegistryDirectory<T> directory, Cluster cluster, Invoker<T> invoker, URL url) {
        this.directory = directory;
        this.cluster = cluster;
        this.invoker = invoker;
        this.url = url;
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setInvoker(Invoker<T> invoker) {
        this.invoker = invoker;
    }

    public RegistryDirectory<T> getDirectory() {
        return directory;
    }

    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public void destroy() {
        invoker.destroy();
    }
}
