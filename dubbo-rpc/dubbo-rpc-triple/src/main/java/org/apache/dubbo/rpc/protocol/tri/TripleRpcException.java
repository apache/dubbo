package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.RpcException;

public class TripleRpcException extends RpcException {
    private final GrpcStatus status;

    public TripleRpcException(GrpcStatus status, String msg) {
        super(msg);
        this.status=status;
    }
    public TripleRpcException(GrpcStatus status, Throwable t) {
        super(t);
        this.status = status;
    }

    public GrpcStatus getStatus() {
        return status;
    }
}
