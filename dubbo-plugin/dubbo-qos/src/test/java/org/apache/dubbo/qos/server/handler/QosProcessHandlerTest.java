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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.QosConfiguration;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class QosProcessHandlerTest {
    @Test
    void testDecodeHttp() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {'G'});
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(context.pipeline()).thenReturn(pipeline);
        QosProcessHandler handler = new QosProcessHandler(FrameworkModel.defaultModel(),
            QosConfiguration.builder()
                .welcome("welcome")
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist(StringUtils.EMPTY_STRING)
                .build()
        );
        handler.decode(context, buf, Collections.emptyList());
        verify(pipeline).addLast(any(HttpServerCodec.class));
        verify(pipeline).addLast(any(HttpObjectAggregator.class));
        verify(pipeline).addLast(any(HttpProcessHandler.class));
        verify(pipeline).remove(handler);
    }

    @Test
    void testDecodeTelnet() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {'A'});
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(context.pipeline()).thenReturn(pipeline);
        QosProcessHandler handler = new QosProcessHandler(FrameworkModel.defaultModel(),
            QosConfiguration.builder()
                .welcome("welcome")
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist(StringUtils.EMPTY_STRING)
                .build()
        );
        handler.decode(context, buf, Collections.emptyList());
        verify(pipeline).addLast(any(LineBasedFrameDecoder.class));
        verify(pipeline).addLast(any(StringDecoder.class));
        verify(pipeline).addLast(any(StringEncoder.class));
        verify(pipeline).addLast(any(TelnetProcessHandler.class));
        verify(pipeline).remove(handler);
    }


}
