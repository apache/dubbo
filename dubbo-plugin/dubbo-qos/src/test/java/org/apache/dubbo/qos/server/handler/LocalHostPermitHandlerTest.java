package org.apache.dubbo.qos.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalHostPermitHandlerTest {
    @Test
    public void testHandlerAdded() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(context.channel()).thenReturn(channel);
        InetAddress addr = mock(InetAddress.class);
        when(addr.isLoopbackAddress()).thenReturn(false);
        InetSocketAddress address = new InetSocketAddress(addr, 12345);
        when(channel.remoteAddress()).thenReturn(address);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(ByteBuf.class))).thenReturn(future);
        LocalHostPermitHandler handler = new LocalHostPermitHandler(false);
        handler.handlerAdded(context);
        ArgumentCaptor<ByteBuf> captor = ArgumentCaptor.forClass(ByteBuf.class);
        verify(context).writeAndFlush(captor.capture());
        assertThat(new String(captor.getValue().array()), containsString("Foreign Ip Not Permitted"));
        verify(future).addListener(ChannelFutureListener.CLOSE);
    }
}
