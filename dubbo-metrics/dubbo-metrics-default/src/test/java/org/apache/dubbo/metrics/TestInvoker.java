package org.apache.dubbo.metrics;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

public class TestInvoker implements Invoker {

    private String side;

    public TestInvoker(String side) {
        this.side = side;
    }

    @Override
    public Class getInterface() {
        return null;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return null;
    }

    @Override
    public URL getUrl() {
        return URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side="+side);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {

    }
}
