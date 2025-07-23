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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class CtrlCHandler extends SimpleChannelInboundHandler<ByteBuf> {
    /**
     * When type 'Ctrl+C', telnet client will send the following sequence:
     * 'FF F4 FF FD 06', it can be divided into two parts:
     * <p>
     * 1. 'FF F4' is telnet interrupt process command.
     * <p>
     * 2. 'FF FD 06' is  to suppress the output of the process that is to be
     *    interrupted by the  interrupt process command.
     * <p>
     * We need to response with 'FF FC 06' to ignore it and tell the client continue
     * display output.
     */
    private byte[] CTRLC_BYTES_SEQUENCE = new byte[] {(byte) 0xff, (byte) 0xf4, (byte) 0xff, (byte) 0xfd, (byte) 0x06};

    private byte[] RESPONSE_SEQUENCE = new byte[] {(byte) 0xff, (byte) 0xfc, 0x06};

    public CtrlCHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        // find ctrl+c
        final int readerIndex = buffer.readerIndex();
        for (int i = readerIndex; i < buffer.writerIndex(); i++) {
            if (buffer.readableBytes() - i < CTRLC_BYTES_SEQUENCE.length) {
                break;
            }
            boolean match = true;
            for (int j = 0; j < CTRLC_BYTES_SEQUENCE.length; j++) {
                if (CTRLC_BYTES_SEQUENCE[j] != buffer.getByte(i + j)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                buffer.readerIndex(readerIndex + buffer.readableBytes());
                ctx.writeAndFlush(Unpooled.wrappedBuffer(RESPONSE_SEQUENCE));
                ctx.writeAndFlush(Unpooled.wrappedBuffer(
                        (QosConstants.BR_STR + QosProcessHandler.PROMPT).getBytes(CharsetUtil.UTF_8)));

                ReferenceCountUtil.release(buffer);
                return;
            }
        }
        ctx.fireChannelRead(buffer);
    }
}
