package org.apache.dubbo.remoting.transport.disruptor;

import com.lmax.disruptor.EventTranslator;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;

public class ChannelEventTranslator implements EventTranslator<ChannelEventRunnable> {

    private Channel channel;
    private ChannelEventRunnable.ChannelState state;
    private Throwable exception;
    private Object message;
    private ChannelHandler handler;

    ChannelEventTranslator() {
    }

    @Override
    public void translateTo(ChannelEventRunnable event, long sequence) {
//        event = new ChannelEventRunnable(this.channel, this.handler, this.state, this.message, this.exception);
//        this.setEventValues(event.handler, event.channel, event.state, event.exception, event.message);
        event.channel = this.channel;
        event.handler = this.handler;
        event.message = this.message;
        event.state = this.state;
        event.exception = this.exception;
        this.clear();
    }

    private void clear(){
        this.setEventValues(null, null, null, null, null);
    }

    public void setEventValues(ChannelHandler handler, Channel channel, ChannelEventRunnable.ChannelState state, Throwable throwable, Object message){
        this.channel = channel;
        this.state = state;
        this.exception = throwable;
        this.message = message;
        this.handler = handler;
    }
}
