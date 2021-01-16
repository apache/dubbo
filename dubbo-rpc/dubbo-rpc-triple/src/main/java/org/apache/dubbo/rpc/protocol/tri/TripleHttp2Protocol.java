package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.netty4.Http2WireProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.ssl.SslContext;

@Activate
public class TripleHttp2Protocol extends Http2WireProtocol {


    @Override
    public void close() {
        super.close();
    }

    @Override
    public void configServerPipeline(ChannelPipeline pipeline) {
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forServer()
                .gracefulShutdownTimeoutMillis(10000)
                .frameLogger(SERVER_LOGGER)
                .build();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new TripleServerInitializer());
        pipeline.addLast(codec, new GracefulShutdownHandler(), handler);
    }

    @Override
    public void configClientPipeline(ChannelPipeline pipeline, SslContext sslContext) {
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forClient()
                .gracefulShutdownTimeoutMillis(10000)
                .frameLogger(CLIENT_LOGGER)
                .build();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                // eat all
            }
        });
        pipeline.addLast(codec, handler, new TripleClientOutboundHandler());
    }
}
