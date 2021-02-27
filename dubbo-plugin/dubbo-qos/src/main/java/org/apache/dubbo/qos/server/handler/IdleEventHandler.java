package org.apache.dubbo.qos.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

@ChannelHandler.Sharable
public class IdleEventHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(IdleEventHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // server will close channel when server don't receive any request from client util timeout.
        if (evt instanceof IdleStateEvent) {
            Channel channel = ctx.channel();
            log.info("IdleStateEvent triggered, close channel " + channel);
            channel.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
