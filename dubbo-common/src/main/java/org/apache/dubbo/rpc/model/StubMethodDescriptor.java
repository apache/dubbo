package org.apache.dubbo.rpc.model;

import java.io.IOException;

public class StubMethodDescriptor extends MethodDescriptor {
    public final ServiceDescriptor serviceDescriptor;
    private final Pack requestPack;
    private final Pack responsePack;
    private final UnPack responseUnpack;
    private final UnPack requestUnpack;
    public final String fullMethodName;
    public final RpcType rpcType;

    public enum RpcType {
        UNARY,
        CLIENT_STREAM,
        SERVER_STREAM,
        BI_STREAM
    }

    public Object parseRequest(byte[] data) throws IOException {
        return requestUnpack.unpack(data);
    }

    public StubMethodDescriptor(String methodName,
                                Class<?> requestClass,
                                Class<?> responseClass,
                                ServiceDescriptor serviceDescriptor,
                                RpcType rpcType,
                                Pack requestPack,
                                Pack responsePack,
                                UnPack requestUnpack,
                                UnPack responseUnpack,
                                String fullMethodName) {
        super(methodName,new Class<?>[]{requestClass},responseClass);
        this.serviceDescriptor = serviceDescriptor;
        this.rpcType = rpcType;
        this.requestPack = requestPack;
        this.responsePack = responsePack;
        this.responseUnpack = responseUnpack;
        this.requestUnpack = requestUnpack;
        this.fullMethodName = fullMethodName;
    }

    public interface Pack {
        byte[] pack(Object obj) throws IOException;
    }

    public interface UnPack {
        Object unpack(byte[] data) throws IOException;
    }
}
