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

import org.apache.dubbo.qos.common.QosConstants;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CtrlCHandlerTest {
    private byte[] CTRLC_BYTES_SEQUENCE = new byte[] {(byte) 0xff, (byte) 0xf4, (byte) 0xff, (byte) 0xfd, (byte) 0x06};

    private byte[] RESPONSE_SEQUENCE = new byte[] {(byte) 0xff, (byte) 0xfc, 0x06};

    @Test
    void testMatchedExactly() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        CtrlCHandler ctrlCHandler = new CtrlCHandler();
        ctrlCHandler.channelRead(context, Unpooled.wrappedBuffer(CTRLC_BYTES_SEQUENCE));
        verify(context).writeAndFlush(Unpooled.wrappedBuffer(RESPONSE_SEQUENCE));
        verify(context)
                .writeAndFlush(Unpooled.wrappedBuffer(
                        (QosConstants.BR_STR + QosProcessHandler.PROMPT).getBytes(CharsetUtil.UTF_8)));
    }

    @Test
    void testMatchedNotExactly() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        CtrlCHandler ctrlCHandler = new CtrlCHandler();
        // before 'ctrl c', user typed other command like 'help'
        String arbitraryCommand = "help";
        byte[] commandBytes = arbitraryCommand.getBytes(StandardCharsets.UTF_8);
        ctrlCHandler.channelRead(context, Unpooled.wrappedBuffer(commandBytes, CTRLC_BYTES_SEQUENCE));
        verify(context).writeAndFlush(Unpooled.wrappedBuffer(RESPONSE_SEQUENCE));
        verify(context)
                .writeAndFlush(Unpooled.wrappedBuffer(
                        (QosConstants.BR_STR + QosProcessHandler.PROMPT).getBytes(CharsetUtil.UTF_8)));
    }

    @Test
    void testNotMatched() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        CtrlCHandler ctrlCHandler = new CtrlCHandler();
        String arbitraryCommand = "help" + QosConstants.BR_STR;
        byte[] commandBytes = arbitraryCommand.getBytes(StandardCharsets.UTF_8);
        ctrlCHandler.channelRead(context, Unpooled.wrappedBuffer(commandBytes));
        verify(context, never()).writeAndFlush(Unpooled.wrappedBuffer(RESPONSE_SEQUENCE));
        verify(context, never())
                .writeAndFlush(Unpooled.wrappedBuffer(
                        (QosConstants.BR_STR + QosProcessHandler.PROMPT).getBytes(CharsetUtil.UTF_8)));
    }
}
