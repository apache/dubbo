package org.apache.dubbo.rpc.protocol.tri;

import io.netty.handler.codec.http2.Http2Headers;

import java.io.InputStream;

public interface Stream {

    void onHeaders(Http2Headers headers);

    void onData(InputStream in);

    void onError(GrpcStatus status);


    void halfClose();
}
