package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.netty4.Http2WireProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;

@Activate
public class TripleHttp2Protocol extends Http2WireProtocol {


    @Override
    public void close() {
        super.close();
    }

    @Override
    public void configServerPipeline(ChannelHandlerContext ctx) {
        final ChannelPipeline p = ctx.pipeline();
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forServer()
                .frameLogger(SERVER_LOGGER)
                .build();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new TripleHttp2Handler());
        p.addLast(codec, handler);
    }

    @Override
    public void configClientPipeline(ChannelHandlerContext ctx) {

    }
}
