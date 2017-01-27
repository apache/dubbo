package com.alibaba.dubbo.rpc.protocol.redis2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RedisCommandEncoder extends MessageToByteEncoder<Command> {

  @Override
  public void encode(ChannelHandlerContext channelHandlerContext, Command command, ByteBuf byteBuf) throws Exception {
    command.write(byteBuf);
  }
}
