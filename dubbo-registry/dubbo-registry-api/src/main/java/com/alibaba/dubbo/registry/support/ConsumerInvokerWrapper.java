package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.integration.RegistryDirectory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * @author qinliujie
 * @date 2017/11/23
 */
public class ConsumerInvokerWrapper<T> implements Invoker {
    private Invoker<T> invoker;
    private URL originUrl;
    private URL registryUrl;
    private URL consumerUrl;
    private RegistryDirectory registryDirectory;

    public ConsumerInvokerWrapper(Invoker<T> invoker, URL registryUrl, URL consumerUrl, RegistryDirectory registryDirectory) {
        this.invoker = invoker;
        this.originUrl = URL.valueOf(invoker.getUrl().toFullString());
        this.registryUrl = URL.valueOf(registryUrl.toFullString());
        this.consumerUrl = consumerUrl;
        this.registryDirectory = registryDirectory;
    }

    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    public URL getUrl() {
        return invoker.getUrl();
    }

    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    public Result invoke(Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    public void destroy() {
        invoker.destroy();
    }

    public URL getOriginUrl() {
        return originUrl;
    }

    public URL getRegistryUrl() {
        return registryUrl;
    }

    public Invoker<T> getInvoker() {
        return invoker;
    }

    public URL getConsumerUrl() {
        return consumerUrl;
    }

    public RegistryDirectory getRegistryDirectory() {
        return registryDirectory;
    }
}
