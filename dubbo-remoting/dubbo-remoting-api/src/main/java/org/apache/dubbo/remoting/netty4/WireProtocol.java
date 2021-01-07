package org.apache.dubbo.remoting.netty4;

import org.apache.dubbo.common.extension.SPI;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;

@SPI
public interface WireProtocol {

    ProtocolDetector detector();

    void configServerPipeline(ChannelPipeline pipeline);

    void configClientPipeline(ChannelPipeline pipeline, SslContext sslContext);

    void close();
}
