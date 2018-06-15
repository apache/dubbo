package org.apache.dubbo.compatible.filter;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;


public class MyFilter implements Filter {

    public static int count = 0;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        count++;

        if (invocation.getArguments()[0].equals("aa")) {
            throw new RpcException(new IllegalArgumentException("arg0 illegal"));
        }

        Result tmp = invoker.invoke(invocation);
        return tmp;
    }
}
