package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.exchange.support.MultiMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * {@link MultiMessageHandler}
 */
public class MultiMessageHandlerTest {

    @Test
    public void test() throws Exception {
        ChannelHandler handler = Mockito.mock(ChannelHandler.class);
        Channel channel = Mockito.mock(Channel.class);
        MultiMessageHandler multiMessageHandler = new MultiMessageHandler(handler);

        MultiMessage multiMessage = MultiMessage.createFromArray("test1", "test2");
        multiMessageHandler.received(channel, multiMessage);
        // verify
        ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);
        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(handler, Mockito.times(2)).received(channelArgumentCaptor.capture(), objectArgumentCaptor.capture());
        Assertions.assertEquals(objectArgumentCaptor.getAllValues().get(0), "test1");
        Assertions.assertEquals(objectArgumentCaptor.getAllValues().get(1), "test2");
        Assertions.assertEquals(channelArgumentCaptor.getValue(), channel);

        Object obj = new Object();
        multiMessageHandler.received(channel, obj);
        // verify
        Mockito.verify(handler, Mockito.times(3)).received(channelArgumentCaptor.capture(), objectArgumentCaptor.capture());
        Assertions.assertEquals(objectArgumentCaptor.getValue(), obj);
        Assertions.assertEquals(channelArgumentCaptor.getValue(), channel);

        RuntimeException runtimeException = new RuntimeException();
        Mockito.doThrow(runtimeException).when(handler).received(Mockito.any(), Mockito.any());
        multiMessageHandler.received(channel, multiMessage);
        // verify
        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(handler, Mockito.times(2)).caught(channelArgumentCaptor.capture(), throwableArgumentCaptor.capture());
        Assertions.assertEquals(throwableArgumentCaptor.getAllValues().get(0), runtimeException);
        Assertions.assertEquals(throwableArgumentCaptor.getAllValues().get(1), runtimeException);
        Assertions.assertEquals(channelArgumentCaptor.getValue(), channel);

    }
}
