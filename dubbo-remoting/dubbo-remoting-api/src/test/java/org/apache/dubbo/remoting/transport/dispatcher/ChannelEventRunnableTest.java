package org.apache.dubbo.remoting.transport.dispatcher;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * {@link ChannelEventRunnable}
 */
public class ChannelEventRunnableTest {

    @Test
    public void test() throws Exception {
        ChannelEventRunnable.ChannelState[] values = ChannelEventRunnable.ChannelState.values();
        Assertions.assertEquals(Arrays.toString(values), "[CONNECTED, DISCONNECTED, SENT, RECEIVED, CAUGHT]");

        Channel channel = Mockito.mock(Channel.class);
        ChannelHandler handler = Mockito.mock(ChannelHandler.class);
        ChannelEventRunnable connectRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.CONNECTED);
        ChannelEventRunnable disconnectRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.DISCONNECTED);
        ChannelEventRunnable sentRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.SENT);
        ChannelEventRunnable receivedRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.RECEIVED, "");
        ChannelEventRunnable caughtRunnable = new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.CAUGHT, new RuntimeException());

        connectRunnable.run();
        disconnectRunnable.run();
        sentRunnable.run();
        receivedRunnable.run();
        caughtRunnable.run();

        ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);
        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(handler, Mockito.times(1)).connected(channelArgumentCaptor.capture());
        Mockito.verify(handler, Mockito.times(1)).disconnected(channelArgumentCaptor.capture());
        Mockito.verify(handler, Mockito.times(1)).sent(channelArgumentCaptor.capture(), Mockito.any());
        Mockito.verify(handler, Mockito.times(1)).received(channelArgumentCaptor.capture(), objectArgumentCaptor.capture());
        Mockito.verify(handler, Mockito.times(1)).caught(channelArgumentCaptor.capture(), throwableArgumentCaptor.capture());
    }
}
