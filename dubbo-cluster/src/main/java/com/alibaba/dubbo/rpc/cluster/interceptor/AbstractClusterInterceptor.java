package com.alibaba.dubbo.rpc.cluster.interceptor;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker;

/**
 * Created by bruce on 2020/1/14 11:11
 */
public abstract class AbstractClusterInterceptor implements ClusterInterceptor {

    public Result intercept(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) throws RpcException {
        return clusterInvoker.invoke(invocation);
    }

}
