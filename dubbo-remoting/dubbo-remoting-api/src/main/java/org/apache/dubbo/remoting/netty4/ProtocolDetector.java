package org.apache.dubbo.remoting.netty4;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Determine incoming bytes belong to the specific protocol.
 *
 * @author guohaoice@gmail.com
 */
public interface ProtocolDetector {

  Result detect(final ChannelHandlerContext ctx, final ByteBuf in);

  enum Result {
    RECOGNIZED, UNRECOGNIZED, NEED_MORE_DATA
  }
}
