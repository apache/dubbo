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
package org.apache.dubbo.remoting.transport.grizzly;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.buffer.DynamicChannelBuffer;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

import static org.apache.dubbo.remoting.Constants.BUFFER_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_BUFFER_SIZE;
import static org.apache.dubbo.remoting.Constants.MAX_BUFFER_SIZE;
import static org.apache.dubbo.remoting.Constants.MIN_BUFFER_SIZE;

/**
 * GrizzlyCodecAdapter
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
        int b = url.getPositiveParameter(BUFFER_KEY, DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= MIN_BUFFER_SIZE && b <= MAX_BUFFER_SIZE ? b : DEFAULT_BUFFER_SIZE;
    }

    @Override
    public NextAction handleWrite(FilterChainContext context) throws IOException {
        Connection<?> connection = context.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer(1024); // Do not need to close

            Object msg = context.getMessage();
            codec.encode(channel, channelBuffer, msg);

            GrizzlyChannel.removeChannelIfDisconnected(connection);
            Buffer buffer = connection.getTransport().getMemoryManager().allocate(channelBuffer.readableBytes());
            buffer.put(channelBuffer.toByteBuffer());
            buffer.flip();
            buffer.allowBufferDispose(true);
            context.setMessage(buffer);
        } finally {
            GrizzlyChannel.removeChannelIfDisconnected(connection);
        }
        return context.getInvokeAction();
    }

    @Override
    public NextAction handleRead(FilterChainContext context) throws IOException {
        Object message = context.getMessage();
        Connection<?> connection = context.getConnection();
        Channel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            if (message instanceof Buffer) { // receive a new packet
                Buffer grizzlyBuffer = (Buffer) message; // buffer

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
            } else { // Other events are passed down directly
                return context.getInvokeAction();
            }
        } finally {
            GrizzlyChannel.removeChannelIfDisconnected(connection);
        }
    }

}