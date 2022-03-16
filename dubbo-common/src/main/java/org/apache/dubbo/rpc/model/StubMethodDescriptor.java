package org.apache.dubbo.rpc.model;

import java.io.IOException;

public class StubMethodDescriptor extends MethodDescriptor {
    private final Pack requestPack;
    private final Pack responsePack;
    private final UnPack responseUnpack;
    private final UnPack requestUnpack;
    public final String fullMethodName;

    public StubMethodDescriptor(String methodName,
                                Pack requestPack,
                                Pack responsePack,
                                UnPack responseUnpack,
                                UnPack requestUnpack,
                                String fullMethodName) {
        super(methodName);
        this.requestPack = requestPack;
        this.responsePack = responsePack;
        this.responseUnpack = responseUnpack;
        this.requestUnpack = requestUnpack;
        this.fullMethodName = fullMethodName;
    }

    interface Pack {
        byte[] pack(Object obj) throws IOException;
    }

    interface UnPack {
        Object pack(byte[] data) throws IOException;
    }
}
