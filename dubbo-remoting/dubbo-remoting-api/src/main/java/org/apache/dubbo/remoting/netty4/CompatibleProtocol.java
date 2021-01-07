package org.apache.dubbo.remoting.netty4;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;

public class CompatibleProtocol implements WireProtocol {
    private final ProtocolDetector detector = new CompatibleProtocolDetector();

    @Override
    public ProtocolDetector detector() {
        return detector;
    }

    @Override
    public void configServerPipeline(ChannelPipeline pipeline) {

    }

    @Override
    public void configClientPipeline(ChannelPipeline pipeline, SslContext sslContext) {

    }


    @Override
    public void close() {

    }
}
