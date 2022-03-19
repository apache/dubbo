package org.apache.dubbo.rpc.model;

import java.io.IOException;

public interface PackableMethod {

    interface Pack {
        byte[] pack(Object obj) throws IOException;
    }

    interface UnPack {
        Object unpack(byte[] data) throws IOException, ClassNotFoundException;
    }

    default Object parseRequest(byte[] data) throws IOException, ClassNotFoundException {
        return getRequestUnpack().unpack(data);
    }

    default Object parseResponse(byte[] data) throws IOException, ClassNotFoundException {
        return getResponseUnpack().unpack(data);
    }

    default byte[] packRequest(Object request) throws IOException {
        return getRequestPack().pack(request);
    }

    default byte[] packResponse(Object response) throws IOException {
        return getResponsePack().pack(response);
    }

    Pack getRequestPack();

    Pack getResponsePack();

    UnPack getResponseUnpack();

    UnPack getRequestUnpack();

    default boolean singleArgument() {
        return true;
    }
}
