package org.apache.dubbo.rpc.protocol.tri;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.SslContext;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.netty4.Http2WireProtocol;


public class GracefulShutdownHandler extends Http2FrameCodec {

    public GracefulShutdownHandler() {
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);
        new GracefulShutdown(ctx,"app_requested", null).gracefulShutdown();
    }
}
