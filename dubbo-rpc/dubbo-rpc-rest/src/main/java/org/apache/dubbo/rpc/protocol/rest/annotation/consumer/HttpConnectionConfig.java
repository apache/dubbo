package org.apache.dubbo.rpc.protocol.rest.annotation.consumer;


import org.apache.dubbo.common.serialize.Constants;

public class HttpConnectionConfig {

    private static HttpConnectionConfig instance;
    private int connectTimeout;
    private int readTimeout;
    private int chunkLength = 8196;
    private byte serialization = Constants.FASTJSON2_SERIALIZATION_ID;


    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getChunkLength() {
        return chunkLength;
    }

    public byte getSerialization() {
        return serialization;
    }



}
