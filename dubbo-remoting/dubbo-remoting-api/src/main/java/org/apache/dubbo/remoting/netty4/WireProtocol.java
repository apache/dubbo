package org.apache.dubbo.remoting.netty4;

import io.netty.channel.ChannelHandlerContext;
import org.apache.dubbo.common.extension.SPI;

@SPI
public interface WireProtocol {

  ProtocolDetector detector();

  void configServerPipeline(ChannelHandlerContext ctx);

  void configClientPipeline(ChannelHandlerContext ctx);
}
