package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.netty4.Http2WireProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2FrameListener;

@Activate
public class TripleHttp2Protocol extends Http2WireProtocol {

    @Override
    protected Http2FrameListener frameListener() {
        return new GrpcHttp2FrameListener();
    }

    @Override
    protected void configServerPipeline0(ChannelHandlerContext ctx) {
        // response -> stream headers/data
        ctx.pipeline().addLast(new GrpcHttp2ServerResponseHandler());
    }
}
