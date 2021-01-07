package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.RpcException;

public class TripleRpcException extends RpcException {
    private final GrpcStatus status;

    public TripleRpcException(GrpcStatus status) {
        super(status.description, status.cause);
        this.status = status;
    }

    public GrpcStatus getStatus() {
        return status;
    }
}
