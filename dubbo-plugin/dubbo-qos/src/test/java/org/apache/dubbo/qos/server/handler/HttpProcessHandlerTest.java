package org.apache.dubbo.qos.server.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpProcessHandlerTest {
    @Test
    public void test1() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(FullHttpResponse.class))).thenReturn(future);
        HttpRequest message = Mockito.mock(HttpRequest.class);
        when(message.getUri()).thenReturn("test");
        HttpProcessHandler handler = new HttpProcessHandler();
        handler.channelRead0(context, message);
        verify(future).addListener(ChannelFutureListener.CLOSE);
        ArgumentCaptor<FullHttpResponse> captor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(context).writeAndFlush(captor.capture());
        FullHttpResponse response = captor.getValue();
        assertThat(response.getStatus().code(), equalTo(404));
    }

    @Test
    public void test2() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(FullHttpResponse.class))).thenReturn(future);
        HttpRequest message = Mockito.mock(HttpRequest.class);
        when(message.getUri()).thenReturn("localhost:80/greeting");
        when(message.getMethod()).thenReturn(HttpMethod.GET);
        HttpProcessHandler handler = new HttpProcessHandler();
        handler.channelRead0(context, message);
        verify(future).addListener(ChannelFutureListener.CLOSE);
        ArgumentCaptor<FullHttpResponse> captor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(context).writeAndFlush(captor.capture());
        FullHttpResponse response = captor.getValue();
        assertThat(response.getStatus().code(), equalTo(200));
    }

    @Test
    public void test3() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(FullHttpResponse.class))).thenReturn(future);
        HttpRequest message = Mockito.mock(HttpRequest.class);
        when(message.getUri()).thenReturn("localhost:80/test");
        when(message.getMethod()).thenReturn(HttpMethod.GET);
        HttpProcessHandler handler = new HttpProcessHandler();
        handler.channelRead0(context, message);
        verify(future).addListener(ChannelFutureListener.CLOSE);
        ArgumentCaptor<FullHttpResponse> captor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(context).writeAndFlush(captor.capture());
        FullHttpResponse response = captor.getValue();
        assertThat(response.getStatus().code(), equalTo(404));
    }
}
