package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.netty4.Http2WireProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2OutboundFrameLogger;

@Activate
public class TripleHttp2Protocol extends Http2WireProtocol {


    @Override
    public void close() {
        super.close();
    }

    @Override
    public void configServerPipeline(ChannelHandlerContext ctx) {
        final ChannelPipeline p = ctx.pipeline();
        final Http2Connection connection = new DefaultHttp2Connection(true);
        final Http2FrameWriter frameWriter= new Http2OutboundFrameLogger(new DefaultHttp2FrameWriter(),Http2WireProtocol.SERVER_LOGGER);
        final Http2ConnectionEncoder encoder=new DefaultHttp2ConnectionEncoder(connection,frameWriter);
        final Http2ConnectionHandler handler = new DubboConnectionHandlerBuilder()
                .connection(connection)
                .frameLogger(SERVER_LOGGER)
                .frameListener(new TripleHttp2FrameListener(connection, frameWriter, encoder))
                .build();

        p.addLast(handler);
    }
}
