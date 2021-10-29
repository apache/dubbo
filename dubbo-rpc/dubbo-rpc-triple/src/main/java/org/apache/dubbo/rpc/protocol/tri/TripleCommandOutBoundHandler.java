package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * @author earthchen
 * @date 2021/10/29
 **/
public class TripleCommandOutBoundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof QueuedCommand.AbstractQueuedCommand) {
            QueuedCommand.AbstractQueuedCommand command = (QueuedCommand.AbstractQueuedCommand) msg;
            command.send(ctx, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
