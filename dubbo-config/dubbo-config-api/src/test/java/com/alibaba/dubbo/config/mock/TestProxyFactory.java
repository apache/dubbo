package com.alibaba.dubbo.config.mock;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.proxy.jdk.JdkProxyFactory;

public class TestProxyFactory extends JdkProxyFactory {
    public static int count = 0;

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        count++;
        return super.getInvoker(proxy, type, url);
    }
}
