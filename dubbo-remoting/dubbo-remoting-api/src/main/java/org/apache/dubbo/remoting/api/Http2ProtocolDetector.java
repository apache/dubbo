package org.apache.dubbo.remoting.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2CodecUtil;

import static java.lang.Math.min;

public class Http2ProtocolDetector implements ProtocolDetector {
    private final ByteBuf clientPrefaceString = Http2CodecUtil.connectionPrefaceBuf();

    @Override
    public Result detect(ChannelHandlerContext ctx, ByteBuf in) {
        int prefaceLen = clientPrefaceString.readableBytes();
        int bytesRead = min(in.readableBytes(), prefaceLen);

        // If the input so far doesn't match the preface, break the connection.
        if (bytesRead == 0 || !ByteBufUtil.equals(in, 0,
                clientPrefaceString, 0, bytesRead)) {

            return Result.UNRECOGNIZED;
        }
        if (bytesRead == prefaceLen) {
            return Result.RECOGNIZED;
        }
        return Result.NEED_MORE_DATA;
    }
}
