package org.apache.dubbo.remoting.transport.netty4;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import io.netty.incubator.codec.quic.QuicChannel;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

@ChannelHandler.Sharable
public class NettyHttp3ConnectionHandler extends NettyConnectionHandler {
    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(NettyHttp3ConnectionHandler.class);

    public NettyHttp3ConnectionHandler(NettyHttp3ConnectionClient connectionClient) {
        super(connectionClient);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
        QuicChannel channel = (QuicChannel) ctx.channel();
        if (!connectionClient.isClosed()) {
            connectionClient.onConnected(ctx.channel());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("The connection of " + channel.localAddress() + " -> " + channel.remoteAddress()
                        + " is established.");
            }
        } else {
            ctx.close();
        }
    }
}
