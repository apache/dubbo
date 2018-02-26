package com.alibaba.dubbo.demo.provider.filter;

import com.alibaba.dubbo.rpc.*;

public class DemoFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

}
