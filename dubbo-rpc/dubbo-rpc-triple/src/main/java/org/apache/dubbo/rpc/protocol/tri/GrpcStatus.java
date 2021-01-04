package org.apache.dubbo.rpc.protocol.tri;

/**
 * See https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
 */
public enum GrpcStatus {
    OK(0),
    UNKNOWN(2),
    NOT_FOUND(5),
    RESOURCE_EXHAUSTED(8),
    UNIMPLEMENTED(12),
    INTERNAL(13);

    final int code;

    GrpcStatus(int code){
        this.code=code;
    }

    public static GrpcStatus fromCode(int code){
        for (GrpcStatus value : GrpcStatus.values()) {
            if(value.code==code){
                return value;
            }
        }
        throw new IllegalStateException("Can not find status for code: "+code);
    }
}
