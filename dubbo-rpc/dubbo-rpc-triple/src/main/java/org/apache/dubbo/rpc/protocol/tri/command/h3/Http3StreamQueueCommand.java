package org.apache.dubbo.rpc.protocol.tri.command.h3;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;

public abstract class Http3StreamQueueCommand extends QueuedCommand {
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(Http3StreamQueueCommand.class);

    protected final Future<QuicStreamChannel> streamChannelFuture;

    protected Http3StreamQueueCommand(Future<QuicStreamChannel> streamChannelFuture) {
        this.streamChannelFuture = streamChannelFuture;
    }

    @Override
    public void run(Channel channel) {
        if (streamChannelFuture.isSuccess()) {
            super.run(channel);
            return;
        }
        promise().setFailure(streamChannelFuture.cause());
    }

    @Override
    public Channel channel() {
        Channel ch = null;
        try {
            ch = this.streamChannelFuture.sync().getNow();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
        return ch;
    }
}
