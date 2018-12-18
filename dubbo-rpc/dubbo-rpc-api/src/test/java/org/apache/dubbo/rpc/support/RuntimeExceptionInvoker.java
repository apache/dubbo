package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

public class RuntimeExceptionInvoker extends MyInvoker {

    public RuntimeExceptionInvoker(URL url) {
        super(url);
    }

    public Result invoke(Invocation invocation) throws RpcException {
        throw new RuntimeException("Runtime exception");
    }
}
