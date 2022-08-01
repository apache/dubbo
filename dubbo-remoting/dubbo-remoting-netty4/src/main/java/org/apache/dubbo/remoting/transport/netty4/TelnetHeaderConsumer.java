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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.buffer.HeapChannelBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

// if server send useless data, this handler consumes useless data
public class TelnetHeaderConsumer extends ByteToMessageDecoder {

    private String DubboLogo = "   ___   __  __ ___   ___   ____     " + System.lineSeparator() +
        "  / _ \\ / / / // _ ) / _ ) / __ \\  " + System.lineSeparator() +
        " / // // /_/ // _  |/ _  |/ /_/ /    " + System.lineSeparator() +
        "/____/ \\____//____//____/ \\____/   " + System.lineSeparator();
    private String PROMPT = "dubbo>";

    private ChannelBuffer LogoBuffer = new HeapChannelBuffer(DubboLogo.getBytes());
    private ChannelBuffer PromptBuffer = new HeapChannelBuffer(PROMPT.getBytes());

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ChannelBuffer in1 = new NettyBackedChannelBuffer(in);
        if (in.readableBytes() >= LogoBuffer.readableBytes() + PromptBuffer.readableBytes()){
            if(ChannelBuffers.prefixEquals(in1, LogoBuffer, LogoBuffer.readableBytes())) {
                int count = LogoBuffer.readableBytes();
                while (count > 0) {
                    count--;
                    in1.readByte();
                }

                if(ChannelBuffers.prefixEquals(in1, PromptBuffer, PromptBuffer.readableBytes())) {
                    count = PromptBuffer.readableBytes();
                    while (count > 0) {
                        count--;
                        in1.readByte();
                    }
                }
            }
            ctx.pipeline().remove(this);
            out.add(in.copy());
        }else {
            // hello message have been canceled
            if (in1.readableBytes() > 0 && !ChannelBuffers.prefixEquals(in1, LogoBuffer, in1.readableBytes())) {
                ctx.pipeline().remove(this);
                out.add(in.copy());
            }
        }
    }
}
