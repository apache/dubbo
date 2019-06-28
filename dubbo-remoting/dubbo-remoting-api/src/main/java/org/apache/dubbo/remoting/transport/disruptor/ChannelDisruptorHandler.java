package org.apache.dubbo.remoting.transport.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.ChannelHandlerDelegate;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;

import java.util.concurrent.ThreadFactory;

public class ChannelDisruptorHandler implements ChannelHandlerDelegate {
    protected static final Logger logger = LoggerFactory.getLogger(ChannelDisruptorHandler.class);

    /**
     * Disruptor RingBuffer Size (2 ^ N)
     */
    private static final int DISRUPTOR_RING_BUFFER_SIZE = 1024;
    private volatile Disruptor<ChannelEventRunnable> disruptor;
    private ChannelHandler handler;
    private final ThreadLocal<EventInfo> threadLocalInfo = ThreadLocal.withInitial(() -> new EventInfo(new ChannelEventTranslator()));
    private final ChannelEventFactory FACTORY = new ChannelEventFactory(handler);

    public ChannelDisruptorHandler(ChannelHandler handler) {
        this.handler = handler;
        ThreadFactory factory = new NamedThreadFactory("channel_disruptor_consumer_pool", false);
        this.disruptor = new Disruptor<>(
//                ChannelEvent.FACTORY,
                FACTORY,
                DISRUPTOR_RING_BUFFER_SIZE,
                factory,
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );
        WorkHandler<ChannelEventRunnable>[] workHandlers = new ChannelEventHandler[16];
        for(int i = 0; i < 16 ; i++){
            workHandlers[i] = new ChannelEventHandler();
        }
//        this.disruptor.setDefaultExceptionHandler(new ChannelEventExceptionHandler());
        this.disruptor.handleEventsWithWorkerPool(workHandlers);
        this.disruptor.start();
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        handler.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        handler.sent(channel, message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        try {
            EventInfo info = threadLocalInfo.get();
            if (info == null) {
                info = new EventInfo(new ChannelEventTranslator());
                threadLocalInfo.set(info);
            }
            Disruptor<ChannelEventRunnable> temp = disruptor;
            if (temp == null) {
                logger.error("The Disruptor queue has been closed, and the message is no longer received.");
            } else if (disruptor.getRingBuffer().remainingCapacity() == 0L) {
                logger.warn("The Disruptor has no remaining buffer, use handler instead");
                this.handler.received(channel, message);
            } else {
                info.eventTranslator.setEventValues(handler, channel, ChannelEventRunnable.ChannelState.RECEIVED, null, message);
                try {
                    disruptor.publishEvent(info.eventTranslator);
                } catch (NullPointerException e) {
                    logger.error("The Disruptor queue has been closed, and the message is no longer received.");
                }
            }
        } catch (Exception e) {
            logger.error("disruptor handler error, exception: {}", e);
            handler.received(channel, message);
        }
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }

    @Override
    public ChannelHandler getHandler() {
        return null;
    }

    public void close() {
        if(disruptor != null){
            disruptor.shutdown();
        }
        threadLocalInfo.remove();
    }

    static class EventInfo {
        private final ChannelEventTranslator eventTranslator;

        EventInfo(ChannelEventTranslator eventTranslator) {
            this.eventTranslator = eventTranslator;
        }
    }

//    private static class ChannelEventFactory implements EventFactory<ChannelEventRunnable>
//    {
//
//        @Override
//        public ChannelEventRunnable newInstance()
//        {
//            return new ChannelEventRunnable();
//        }
//    }

    /**
     * Factory used to populate the RingBuffer with events. These event objects are then re-used during the life of the
     * RingBuffer.
     */
    private static class ChannelEventFactory implements EventFactory<ChannelEventRunnable> {
        private ChannelHandler channelHandler;

        ChannelEventFactory(ChannelHandler channelHandler) {
            this.channelHandler = channelHandler;
        }

        @Override
        public ChannelEventRunnable newInstance() {
            return new ChannelEventRunnable(null, channelHandler, null, null);
        }
    }
}
