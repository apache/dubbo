package org.apache.dubbo.rpc.protocol.tri;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class TripleServerInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel ch) throws Exception {
        final ChannelPipeline p = ch.pipeline();
        p.addLast(new TripleHttp2FrameServerHandler());
        // TODO constraint MAX DATA_SIZE
        p.addLast(new GrpcDataDecoder(Integer.MAX_VALUE));
        p.addLast(new TripleInvokeServerHandler());
    }
}
