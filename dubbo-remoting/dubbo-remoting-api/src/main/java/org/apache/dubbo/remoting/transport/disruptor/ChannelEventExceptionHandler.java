package org.apache.dubbo.remoting.transport.disruptor;

import com.lmax.disruptor.ExceptionHandler;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;

public class ChannelEventExceptionHandler implements ExceptionHandler<ChannelEventRunnable> {

    protected static final Logger logger = LoggerFactory.getLogger(ChannelEventExceptionHandler.class);

    @Override
    public void handleEventException(Throwable ex, long sequence, ChannelEventRunnable event) {
        logger.error(ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        logger.error(ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        logger.error(ex);
    }
}