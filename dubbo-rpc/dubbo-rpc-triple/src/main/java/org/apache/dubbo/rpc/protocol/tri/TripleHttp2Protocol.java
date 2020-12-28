package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.netty4.Http2WireProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;

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
        final Http2ConnectionHandler handler = new DubboConnectionHandlerBuilder()
                .connection(connection)
                .frameLogger(SERVER_LOGGER)
                .frameListener(new TripleHttp2FrameListener(connection))
                .build();

        p.addLast(handler);
    }
}
