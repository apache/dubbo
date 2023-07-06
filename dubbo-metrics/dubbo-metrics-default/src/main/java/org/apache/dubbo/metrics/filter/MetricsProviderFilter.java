package org.apache.dubbo.metrics.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

@Activate(group = {PROVIDER}, order = Integer.MIN_VALUE + 100)
public class MetricsProviderFilter extends MetricsFilter implements Filter, BaseFilter.Listener {
    public MetricsProviderFilter() {
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return super.invoke(invoker, invocation, true);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        super.onResponse(appResponse, invoker, invocation, true);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        super.onError(t, invoker, invocation, true);
    }
}
