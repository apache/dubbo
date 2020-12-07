package org.apache.dubbo.remoting.transport.netty4;

import java.util.regex.Pattern;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


@ChannelHandler.Sharable
public class ExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Pattern IGNORABLE_ERROR_MESSAGE = Pattern.compile(
            "^.*(?:connection.*(?:reset|closed|abort|broken)|broken.*pipe).*$", Pattern.CASE_INSENSITIVE);
    public static ExceptionHandler INSTANCE = new ExceptionHandler();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String message = String.valueOf(cause.getMessage()).toLowerCase();
        if (!IGNORABLE_ERROR_MESSAGE.matcher(message).matches()) {
            //log.error("Caught IO Exception in process.", cause);
        }
        if (ctx.channel().isActive()) {
            ctx.channel().close();
        }
    }
}
