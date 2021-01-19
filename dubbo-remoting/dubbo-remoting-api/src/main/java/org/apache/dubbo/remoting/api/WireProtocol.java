package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.api.ProtocolDetector;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;

@SPI
public interface WireProtocol {

    ProtocolDetector detector();

    void configServerPipeline(ChannelPipeline pipeline, SslContext sslContext);

    void configClientPipeline(ChannelPipeline pipeline, SslContext sslContext);

    void close();
}
