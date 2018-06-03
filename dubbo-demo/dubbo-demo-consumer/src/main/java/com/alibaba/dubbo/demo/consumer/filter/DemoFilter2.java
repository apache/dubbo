package com.alibaba.dubbo.demo.consumer.filter;

import com.alibaba.dubbo.rpc.*;

public class DemoFilter2 implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result = invoker.invoke(invocation);
        result.getAttachments().put("hh", "hhhh");
        return result;
    }

}
