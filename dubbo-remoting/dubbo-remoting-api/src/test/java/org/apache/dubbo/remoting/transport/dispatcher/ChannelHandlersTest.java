package org.apache.dubbo.remoting.transport.dispatcher;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.exchange.support.header.HeartbeatHandler;
import org.apache.dubbo.remoting.transport.AbstractChannelHandlerDelegate;
import org.apache.dubbo.remoting.transport.MultiMessageHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

public class ChannelHandlersTest {
    @Test
    public void test() {
        ChannelHandlers instance1 = ChannelHandlers.getInstance();
        ChannelHandlers instance2 = ChannelHandlers.getInstance();
        Assertions.assertEquals(instance1, instance2);

        ChannelHandler channelHandler = Mockito.mock(ChannelHandler.class);
        URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 9999);
        ChannelHandler wrappedHandler = ChannelHandlers.wrap(channelHandler, url);
        Assertions.assertTrue(wrappedHandler instanceof MultiMessageHandler);

        MultiMessageHandler multiMessageHandler = (MultiMessageHandler) wrappedHandler;
        ChannelHandler handler = multiMessageHandler.getHandler();
        Assertions.assertEquals(channelHandler, handler);
    }
}
