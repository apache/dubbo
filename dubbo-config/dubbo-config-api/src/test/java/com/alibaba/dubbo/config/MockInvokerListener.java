package com.alibaba.dubbo.config;

import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.InvokerListener;
import com.alibaba.dubbo.rpc.RpcException;

public class MockInvokerListener implements InvokerListener {
    @Override
    public void referred(Invoker<?> invoker) throws RpcException {

    }

    @Override
    public void destroyed(Invoker<?> invoker) {

    }
}
