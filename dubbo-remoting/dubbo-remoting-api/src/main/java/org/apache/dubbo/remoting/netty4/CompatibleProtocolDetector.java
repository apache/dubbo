package org.apache.dubbo.remoting.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class CompatibleProtocolDetector implements ProtocolDetector {
    @Override
    public Result detect(ChannelHandlerContext ctx, ByteBuf in) {
        return null;
    }
}
