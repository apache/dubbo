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
package com.alibaba.dubbo.remoting.transport.mina;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffers;
import com.alibaba.dubbo.remoting.buffer.DynamicChannelBuffer;

/**
 * MinaCodecAdapter.
 *
 * @author qian.lei
 */
final class MinaCodecAdapter implements ProtocolCodecFactory {

    private final ProtocolEncoder encoder            = new InternalEncoder();

    private final ProtocolDecoder decoder            = new InternalDecoder();

    private final Codec2          codec;

    private final URL             url;

    private final ChannelHandler  handler;

    private final int            bufferSize;

    public MinaCodecAdapter(Codec2 codec, URL url, ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
        int b = url.getPositiveParameter(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= Constants.MIN_BUFFER_SIZE && b <= Constants.MAX_BUFFER_SIZE ? b : Constants.DEFAULT_BUFFER_SIZE;
    }

    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    public ProtocolDecoder getDecoder() {
        return decoder;
    }

    private class InternalEncoder implements ProtocolEncoder {

        public void dispose(IoSession session) throws Exception {
        }

        public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) throws Exception {
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(1024);
            MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
            try {
            	codec.encode(channel, buffer, msg);
            } finally {
                MinaChannel.removeChannelIfDisconnectd(session);
            }
            out.write(ByteBuffer.wrap(buffer.toByteBuffer()));
            out.flush();
        }
    }

    private class InternalDecoder implements ProtocolDecoder {

        private ChannelBuffer buffer = ChannelBuffers.EMPTY_BUFFER;

        public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
            int readable = in.limit();
            if (readable <= 0) return;

            ChannelBuffer frame;

            if (buffer.readable()) {
                if (buffer instanceof DynamicChannelBuffer) {
                    buffer.writeBytes(in.buf());
                    frame = buffer;
                } else {
                    int size = buffer.readableBytes() + in.remaining();
                    frame = ChannelBuffers.dynamicBuffer(size > bufferSize ? size : bufferSize);
                    frame.writeBytes(buffer, buffer.readableBytes());
                    frame.writeBytes(in.buf());
                }
            } else {
                frame = ChannelBuffers.wrappedBuffer(in.buf());
            }

            Channel channel = MinaChannel.getOrAddChannel(session, url, handler);
            Object msg;
            int savedReadIndex;

            try {
                do {
                    savedReadIndex = frame.readerIndex();
                    try {
                        msg = codec.decode(channel, frame);
                    } catch (Exception e) {
                        buffer = ChannelBuffers.EMPTY_BUFFER;
                        throw e;
                    }
                    if (msg == Codec2.DecodeResult.NEED_MORE_INPUT) {
                        frame.readerIndex(savedReadIndex);
                        break;
                    } else {
                        if (savedReadIndex == frame.readerIndex()) {
                            buffer = ChannelBuffers.EMPTY_BUFFER;
                            throw new Exception("Decode without read data.");
                        }
                        if (msg != null) {
                            out.write(msg);
                        }
                    }
                } while (frame.readable());
            } finally {
                if (frame.readable()) {
                    frame.discardReadBytes();
                    buffer = frame;
                } else {
                    buffer = ChannelBuffers.EMPTY_BUFFER;
                }
                MinaChannel.removeChannelIfDisconnectd(session);
            }
        }

        public void dispose(IoSession session) throws Exception {
        }

        public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
        }
    }
}