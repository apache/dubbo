package org.apache.dubbo.rpc.cluster.filter.support;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metrics.filter.MetricsFilter;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

@Activate(group = {CONSUMER}, order = Integer.MIN_VALUE + 100)
public class MetricsConsumerFilter extends MetricsFilter implements ClusterFilter, BaseFilter.Listener {
    public MetricsConsumerFilter() {
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return super.invoke(invoker, invocation, false);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        super.onResponse(appResponse, invoker, invocation, false);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        super.onError(t, invoker, invocation, false);
    }
}
