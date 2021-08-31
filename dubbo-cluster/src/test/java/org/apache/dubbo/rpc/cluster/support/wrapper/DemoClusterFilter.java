package org.apache.dubbo.rpc.cluster.support.wrapper;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

@Activate(value = "demo",group = {CONSUMER})
public class DemoClusterFilter implements ClusterFilter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }
}
