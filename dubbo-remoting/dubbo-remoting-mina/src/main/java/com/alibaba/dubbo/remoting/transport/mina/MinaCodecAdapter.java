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

import java.io.IOException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Codec;

/**
 * MinaCodecAdapter.
 * 
 * @author qian.lei
 */
final class MinaCodecAdapter implements ProtocolCodecFactory {

    private static final String   BUFFER_KEY          = MinaCodecAdapter.class.getName() + ".BUFFER";

    private final ProtocolEncoder encoder            = new InternalEncoder();

    private final ProtocolDecoder decoder            = new InternalDecoder();

    private final Codec           codec;

    private final URL             url;
    
    private final ChannelHandler  handler;

    private final int            bufferSize;
    
    public MinaCodecAdapter(Codec codec, URL url, ChannelHandler handler) {
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
            UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(1024); // 不需要关闭
            MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
            try {
            	codec.encode(channel, os, msg);
            } finally {
                MinaChannel.removeChannelIfDisconnectd(session);
            }
            out.write(ByteBuffer.wrap(os.toByteArray()));
            out.flush();
        }
    }

    private class InternalDecoder implements ProtocolDecoder {

        public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
            int readable = in.limit();
            if (readable <= 0) return;

            int off, limit;
            byte[] buf;
            // load buffer from context.
            Object[] tmp = (Object[]) session.getAttribute(BUFFER_KEY);
            if (tmp == null) {
                buf = new byte[bufferSize];
                off = limit = 0;
            } else {
                buf = (byte[]) tmp[0];
                off = (Integer) tmp[1];
                limit = (Integer) tmp[2];
            }

            Channel channel = MinaChannel.getOrAddChannel(session, url, handler);
            boolean remaining = true;
            Object msg;
            UnsafeByteArrayInputStream bis;
            try {
                do {
                    // read data into buffer.
                    int read = Math.min(readable, buf.length - limit);
                    in.get(buf, limit, read);
                    limit += read;
                    readable -= read;
                    bis = new UnsafeByteArrayInputStream(buf, off, limit - off); // 不需要关闭
                    // decode object.
                    do {
                        try {
                            msg = codec.decode(channel, bis);
                        } catch (IOException e) {
                            remaining = false;
                            throw e;
                        }
                        if (msg == Codec.NEED_MORE_INPUT) {
                            if (off == 0) {
                                if (readable > 0) {
                                    buf = Bytes.copyOf(buf, buf.length << 1);
                                }
                            } else {
                                int len = limit - off;
                                System.arraycopy(buf, off, buf, 0, len);
                                off = 0;
                                limit = len;
                            }
                            break;
                        } else {
                            int pos = bis.position();
                            if (pos == off) {
                                remaining = false;
                                throw new IOException("Decode without read data.");
                            }
                            if (msg != null) {
                                out.write(msg);
                            }
                            off = pos;
                        }
                    } while (bis.available() > 0);
                } while (readable > 0);
            } finally {
                if (remaining) {
                    int len = limit - off;
                    if (len < buf.length / 2) {
                        System.arraycopy(buf, off, buf, 0, len);
                        off = 0;
                        limit = len;
                    }
                    session.setAttribute(BUFFER_KEY, new Object[] { buf, off, limit });
                } else {
                    session.removeAttribute(BUFFER_KEY);
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