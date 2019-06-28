package org.apache.dubbo.remoting.transport.disruptor;

import com.lmax.disruptor.WorkHandler;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;

public class ChannelEventHandler implements WorkHandler<ChannelEventRunnable> {

    private ChannelHandler channelHandler;

    ChannelEventHandler(ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    @Override
    public void onEvent(ChannelEventRunnable channelEvent) throws Exception {
        channelEvent.run();
    }
}