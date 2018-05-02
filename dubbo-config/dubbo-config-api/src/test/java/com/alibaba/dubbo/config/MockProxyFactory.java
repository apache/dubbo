package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;

public class MockProxyFactory implements ProxyFactory {
    @Override
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return null;
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return null;
    }
}
