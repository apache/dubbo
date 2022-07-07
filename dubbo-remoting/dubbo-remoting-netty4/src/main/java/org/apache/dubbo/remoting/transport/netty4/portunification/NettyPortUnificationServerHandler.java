package org.apache.dubbo.remoting.transport.netty4.portunification;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@io.netty.channel.ChannelHandler.Sharable
public class NettyPortUnificationServerHandler extends ChannelDuplexHandler {
    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>();

    private static final Logger logger = LoggerFactory.getLogger(NettyPortUnificationServerHandler.class);

    public Map<String, Channel> getChannels() {
        return channels;
    }


    public NettyPortUnificationServerHandler(ChannelHandler handler) {
        this.handler = handler;
    }

    // this handler will be bind to channelWithHandler and will be updated after recognizing
    private final ChannelHandler handler;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), handler);
        if (channel == null) {
            return;
        }else {
            channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
        }

        channel.connected(channel);

        if (logger.isInfoEnabled()) {
            logger.info("The connection of " + channel.getRemoteAddress() + " -> " + channel.getLocalAddress() + " is established.");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), handler);
        try {
            channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
            channel.disconnected(channel);
        } finally {
            NettyChannelWithHandler.removeChannel(ctx.channel());
        }

        if (logger.isInfoEnabled()) {
            logger.info("The connection of " + channel.getRemoteAddress() + " -> " + channel.getLocalAddress() + " is disconnected.");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), handler);
        if (channel.getUrl() == null) {
            return;
        }
        channel.received(channel, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), handler);
        channel.sent(channel, msg);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // server will close channel when server don't receive any heartbeat from client util timeout.
        if (evt instanceof IdleStateEvent) {
            NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), handler);
            try {
                logger.info("IdleStateEvent triggered, close channel " + channel);
                channel.close();
            } finally {
                NettyChannelWithHandler.removeChannelIfDisconnected(ctx.channel());
            }
        }
        super.userEventTriggered(ctx, evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        throws Exception {
        NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), handler);
        try {
            channel.caught(channel, cause);
        } finally {
            NettyChannelWithHandler.removeChannelIfDisconnected(ctx.channel());
        }
    }
}
