package org.apache.dubbo.rpc.protocol.grpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2FrameListener;
import org.apache.dubbo.remoting.netty4.Http2WireProtocol;

public class GrpcHttp2Protocol extends Http2WireProtocol {

    @Override
    protected Http2FrameListener frameListener() {
        return new GrpcHttp2FrameListener();
    }

    @Override
    protected void configServerPipeline0(ChannelHandlerContext ctx) {
        // response -> data header/data
    }
}
