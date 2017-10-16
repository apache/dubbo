/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport.grizzly;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffers;
import com.alibaba.dubbo.remoting.buffer.DynamicChannelBuffer;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

/**
 * GrizzlyCodecAdapter
 *
 * @author william.liangf
 */
public class GrizzlyCodecAdapter extends BaseFilter {

    private final Codec2 codec;

    private final URL url;

    private final ChannelHandler handler;

    private final int bufferSize;

    private ChannelBuffer previousData = ChannelBuffers.EMPTY_BUFFER;

    public GrizzlyCodecAdapter(Codec2 codec, URL url, ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
        int b = url.getPositiveParameter(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= Constants.MIN_BUFFER_SIZE && b <= Constants.MAX_BUFFER_SIZE ? b : Constants.DEFAULT_BUFFER_SIZE;
    }

    @Override
    public NextAction handleWrite(FilterChainContext context) throws IOException {
        Connection<?> connection = context.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer(1024); // 不需要关闭

            Object msg = context.getMessage();
            codec.encode(channel, channelBuffer, msg);

            GrizzlyChannel.removeChannelIfDisconnectd(connection);
            Buffer buffer = connection.getTransport().getMemoryManager().allocate(channelBuffer.readableBytes());
            buffer.put(channelBuffer.toByteBuffer());
            buffer.flip();
            buffer.allowBufferDispose(true);
            context.setMessage(buffer);
        } finally {
            GrizzlyChannel.removeChannelIfDisconnectd(connection);
        }
        return context.getInvokeAction();
    }

    @Override
    public NextAction handleRead(FilterChainContext context) throws IOException {
        Object message = context.getMessage();
        Connection<?> connection = context.getConnection();
        Channel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            if (message instanceof Buffer) { // 收到新的数据包
                Buffer grizzlyBuffer = (Buffer) message; // 缓存

                ChannelBuffer frame;

                if (previousData.readable()) {
                    if (previousData instanceof DynamicChannelBuffer) {
                        previousData.writeBytes(grizzlyBuffer.toByteBuffer());
                        frame = previousData;
                    } else {
                        int size = previousData.readableBytes() + grizzlyBuffer.remaining();
                        frame = ChannelBuffers.dynamicBuffer(size > bufferSize ? size : bufferSize);
                        frame.writeBytes(previousData, previousData.readableBytes());
                        frame.writeBytes(grizzlyBuffer.toByteBuffer());
                    }
                } else {
                    frame = ChannelBuffers.wrappedBuffer(grizzlyBuffer.toByteBuffer());
                }

                Object msg;
                int savedReadIndex;

                do {
                    savedReadIndex = frame.readerIndex();
                    try {
                        msg = codec.decode(channel, frame);
                    } catch (Exception e) {
                        previousData = ChannelBuffers.EMPTY_BUFFER;
                        throw new IOException(e.getMessage(), e);
                    }
                    if (msg == Codec2.DecodeResult.NEED_MORE_INPUT) {
                        frame.readerIndex(savedReadIndex);
                        return context.getStopAction();
                    } else {
                        if (savedReadIndex == frame.readerIndex()) {
                            previousData = ChannelBuffers.EMPTY_BUFFER;
                            throw new IOException("Decode without read data.");
                        }
                        if (msg != null) {
                            context.setMessage(msg);
                            return context.getInvokeAction();
                        } else {
                            return context.getInvokeAction();
                        }
                    }
                } while (frame.readable());
            } else { // 其它事件直接往下传
                return context.getInvokeAction();
            }
        } finally {
            GrizzlyChannel.removeChannelIfDisconnectd(connection);
        }
    }

}