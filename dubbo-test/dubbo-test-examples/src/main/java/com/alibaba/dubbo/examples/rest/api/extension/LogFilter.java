package com.alibaba.dubbo.examples.rest.api.extension;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * @author Authorlove on 22/11/2017.
 */
public class LogFilter implements Filter {
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println(invocation.getMethodName() + "is invoked");
        return invoker.invoke(invocation);
    }
}
