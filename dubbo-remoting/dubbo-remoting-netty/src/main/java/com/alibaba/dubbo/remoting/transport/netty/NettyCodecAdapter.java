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
package com.alibaba.dubbo.remoting.transport.netty;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.remoting.Codec;

/**
 * NettyCodecAdapter.
 * 
 * @author qian.lei
 */
final class NettyCodecAdapter {

    private final ChannelHandler encoder = new InternalEncoder();
    
    private final ChannelHandler decoder = new InternalDecoder();

    private final Codec          codec;
    
    private final URL            url;
    
    private final int            bufferSize;
    
    private final com.alibaba.dubbo.remoting.ChannelHandler handler;

    public NettyCodecAdapter(Codec codec, URL url, com.alibaba.dubbo.remoting.ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
        int b = url.getPositiveParameter(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= Constants.MIN_BUFFER_SIZE && b <= Constants.MAX_BUFFER_SIZE ? b : Constants.DEFAULT_BUFFER_SIZE;
    }

    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    @Sharable
    private class InternalEncoder extends OneToOneEncoder {

        @Override
        protected Object encode(ChannelHandlerContext ctx, Channel ch, Object msg) throws Exception {
            UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(1024); // 不需要关闭
            NettyChannel channel = NettyChannel.getOrAddChannel(ch, url, handler);
            try {
            	codec.encode(channel, os, msg);
            } finally {
                NettyChannel.removeChannelIfDisconnected(ch);
            }
            return ChannelBuffers.wrappedBuffer(os.toByteBuffer());
        }
    }

    private class InternalDecoder extends SimpleChannelUpstreamHandler {

        private int    mOffset = 0, mLimit = 0;

        private byte[] mBuffer = null;

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
            Object o = event.getMessage();
            if (! (o instanceof ChannelBuffer)) {
                ctx.sendUpstream(event);
                return;
            }

            ChannelBuffer input = (ChannelBuffer) o;
            int readable = input.readableBytes();
            if (readable <= 0) {
                return;
            }

            int off, limit;
            byte[] buf = mBuffer;
            if (buf == null) {
                buf = new byte[bufferSize];
                off = limit = 0;
            } else {
                off = mOffset;
                limit = mLimit;
            }

            NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
            boolean remaining = true;
            Object msg;
            UnsafeByteArrayInputStream bis;
            try {
                do {
                    // read data into buffer.
                    int read = Math.min(readable, buf.length - limit);
                    input.readBytes(buf, limit, read);
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
                                System.arraycopy(buf, off, buf, 0, len); // adjust buffer.
                                off = 0;
                                limit = len;
                            }
                            break;
                        } else {
                            int pos = bis.position();
                            if (off == pos) {
                                remaining = false;
                                throw new IOException("Decode without read data.");
                            }
                            if (msg != null) {
                                Channels.fireMessageReceived(ctx, msg, event.getRemoteAddress());
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
                    mBuffer = buf;
                    mOffset = off;
                    mLimit = limit;
                } else {
                    mBuffer = null;
                    mOffset = mLimit = 0;
                }
                NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            ctx.sendUpstream(e);
        }
    }
}