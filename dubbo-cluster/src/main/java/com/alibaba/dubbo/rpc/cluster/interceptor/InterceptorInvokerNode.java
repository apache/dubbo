package com.alibaba.dubbo.rpc.cluster.interceptor;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import java.util.List;

/**
 * Created by bruce on 2020/1/14 10:33
 */
public class InterceptorInvokerNode<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(InterceptorInvokerNode.class);

    private AbstractClusterInvoker<T> clusterInvoker;
    private ClusterInterceptor interceptor;
    private AbstractClusterInvoker<T> next;

    public InterceptorInvokerNode(AbstractClusterInvoker<T> clusterInvoker,
                                  ClusterInterceptor interceptor,
                                  AbstractClusterInvoker<T> next) {
        this.clusterInvoker = clusterInvoker;
        this.interceptor = interceptor;
        this.next = next;

    }

    @Override
    public Class<T> getInterface() {
        return clusterInvoker.getInterface();
    }

    @Override
    public URL getUrl() {
        return clusterInvoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return clusterInvoker.isAvailable();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        Result result = null;
        try {
            interceptor.before(next, invocation);
            result = interceptor.intercept(next, invocation);
        } catch (Exception e) {
            // onError callback
            if (interceptor instanceof ClusterInterceptor.Listener) {
                ClusterInterceptor.Listener listener = (ClusterInterceptor.Listener) interceptor;
                listener.onError(e, clusterInvoker, invocation);
            } else {
                logger.error(e);
            }
        } finally {
            interceptor.after(next, invocation);
        }

        if (interceptor instanceof ClusterInterceptor.Listener) {
            ClusterInterceptor.Listener listener = (ClusterInterceptor.Listener) interceptor;
            Throwable exception = result.getException();
            if (exception == null) {
                listener.onMessage(result, clusterInvoker, invocation);
            } else {
                listener.onError(exception, clusterInvoker, invocation);
            }
        }

        return result;
    }

    @Override
    public void destroy() {
        clusterInvoker.destroy();
    }

    @Override
    public String toString() {
        return clusterInvoker.toString();
    }

    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        // The only purpose is to build a interceptor chain, so the cluster related logic doesn't matter.
        return null;
    }
}
