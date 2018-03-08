package com.alibaba.dubbo.demo.provider.filter;

import com.alibaba.dubbo.demo.provider.DemoDAO;
import com.alibaba.dubbo.rpc.*;

public class DemoFilter implements Filter {

    private DemoDAO demoDAO;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    public DemoFilter setDemoDAO(DemoDAO demoDAO) {
        this.demoDAO = demoDAO;
        return this;
    }
}
