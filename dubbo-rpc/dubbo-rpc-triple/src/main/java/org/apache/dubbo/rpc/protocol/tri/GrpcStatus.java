package org.apache.dubbo.rpc.protocol.tri;

/**
 * See https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
 */
enum GrpcStatus {
    OK(0),
    UNKNOWN(2),
    NOT_FOUND(5),
    UNIMPLEMENTED(12),
    INTERNAL(13);

    final int code;

    GrpcStatus(int code){
        this.code=code;
    }
}
