package com.alibaba.dubbo.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RpcResult extends AppResponse implements com.alibaba.dubbo.rpc.Result {
    public RpcResult() {
    }

    public RpcResult(Object result) {
        super(result);
    }

    public RpcResult(Throwable exception) {
        super(exception);
    }
}

