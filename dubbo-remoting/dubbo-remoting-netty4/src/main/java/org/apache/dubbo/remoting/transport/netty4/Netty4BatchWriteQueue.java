package org.apache.dubbo.remoting.transport.netty4;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import org.apache.dubbo.common.BatchExecutorQueue;

/**
 * netty4 batch write queue
 *
 * @author icodening
 * @date 2022.10.11
 */
public class Netty4BatchWriteQueue extends BatchExecutorQueue<Netty4BatchWriteQueue.MessageTuple> {

    private final Channel channel;

    private final EventLoop eventLoop;

    private Netty4BatchWriteQueue(Channel channel) {
        this.channel = channel;
        this.eventLoop = channel.eventLoop();
    }

    public ChannelFuture enqueue(Object message) {
        return enqueue(message, channel.newPromise());
    }

    public ChannelFuture enqueue(Object message, ChannelPromise channelPromise) {
        MessageTuple messageTuple = new MessageTuple(message, channelPromise);
        super.enqueue(messageTuple, eventLoop);
        return messageTuple.channelPromise;
    }

    @Override
    protected void prepare(MessageTuple item) {
        channel.write(item.originMessage, item.channelPromise);
    }

    @Override
    protected void flush(MessageTuple item) {
        prepare(item);
        channel.flush();
    }

    public static Netty4BatchWriteQueue createWriteQueue(Channel channel) {
        return new Netty4BatchWriteQueue(channel);
    }

    static class MessageTuple {

        private final Object originMessage;

        private final ChannelPromise channelPromise;

        public MessageTuple(Object originMessage, ChannelPromise channelPromise) {
            this.originMessage = originMessage;
            this.channelPromise = channelPromise;
        }

    }
}
