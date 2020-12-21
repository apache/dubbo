package org.apache.dubbo.remoting.netty4;

import io.netty.channel.ChannelHandlerContext;

public interface WireProtocol {

  ProtocolDetector detector();

  void configServerPipeline(ChannelHandlerContext ctx);

  void configClientPipeline(ChannelHandlerContext ctx);
}
