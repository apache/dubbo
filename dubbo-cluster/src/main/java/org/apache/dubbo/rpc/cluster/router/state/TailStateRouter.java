package org.apache.dubbo.rpc.cluster.router.state;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;

public class TailStateRouter<T> implements StateRouter<T> {
    private static final TailStateRouter INSTANCE = new TailStateRouter();

    @SuppressWarnings("unchecked")
    public static <T> TailStateRouter<T> getInstance() {
        return INSTANCE;
    }

    private TailStateRouter() {

    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder) throws RpcException {
        return invokers;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isForce() {
        return false;
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {

    }
}
