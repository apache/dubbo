package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.ServerService;

public class StubProxyFactory implements ProxyFactory {
    @Override
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return StubSuppliers.createStub(invoker.getInterface()
            .getName(), invoker);
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
        return getProxy(invoker);
    }

    @Override
    @SuppressWarnings("all")
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return ((ServerService) proxy).getInvoker(url);
    }
}
