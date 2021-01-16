package org.apache.dubbo.remoting.netty4;

import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.api.WireProtocol;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;

public class EmptyProtocol implements WireProtocol {
    @Override
    public ProtocolDetector detector() {
        return null;
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
