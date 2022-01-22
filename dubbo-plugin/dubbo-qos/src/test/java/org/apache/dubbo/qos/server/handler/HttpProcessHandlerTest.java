/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.qos.server.handler;

import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
        when(message.uri()).thenReturn("test");
        HttpProcessHandler handler = new HttpProcessHandler(FrameworkModel.defaultModel());
        handler.channelRead0(context, message);
        verify(future).addListener(ChannelFutureListener.CLOSE);
        ArgumentCaptor<FullHttpResponse> captor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(context).writeAndFlush(captor.capture());
        FullHttpResponse response = captor.getValue();
        assertThat(response.status().code(), equalTo(404));
    }

    @Test
    public void test2() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(FullHttpResponse.class))).thenReturn(future);
        HttpRequest message = Mockito.mock(HttpRequest.class);
        when(message.uri()).thenReturn("localhost:80/greeting");
        when(message.method()).thenReturn(HttpMethod.GET);
        HttpProcessHandler handler = new HttpProcessHandler(FrameworkModel.defaultModel());
        handler.channelRead0(context, message);
        verify(future).addListener(ChannelFutureListener.CLOSE);
        ArgumentCaptor<FullHttpResponse> captor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(context).writeAndFlush(captor.capture());
        FullHttpResponse response = captor.getValue();
        assertThat(response.status().code(), equalTo(200));
    }

    @Test
    public void test3() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        ChannelFuture future = mock(ChannelFuture.class);
        when(context.writeAndFlush(any(FullHttpResponse.class))).thenReturn(future);
        HttpRequest message = Mockito.mock(HttpRequest.class);
        when(message.uri()).thenReturn("localhost:80/test");
        when(message.method()).thenReturn(HttpMethod.GET);
        HttpProcessHandler handler = new HttpProcessHandler(FrameworkModel.defaultModel());
        handler.channelRead0(context, message);
        verify(future).addListener(ChannelFutureListener.CLOSE);
        ArgumentCaptor<FullHttpResponse> captor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(context).writeAndFlush(captor.capture());
        FullHttpResponse response = captor.getValue();
        assertThat(response.status().code(), equalTo(404));
    }
}
