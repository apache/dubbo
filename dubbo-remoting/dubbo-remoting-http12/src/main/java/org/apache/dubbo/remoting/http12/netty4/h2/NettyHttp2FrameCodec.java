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
package org.apache.dubbo.remoting.http12.netty4.h2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.message.DefaultHttpHeaders;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

public class NettyHttp2FrameCodec extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttp2FrameCodec.class);

    private final List<CachedMsg> cachedMsgList = new LinkedList<>();

    private boolean settingsFrameArrived;

    public NettyHttp2FrameCodec(NettyHttp2SettingsHandler nettyHttp2SettingsHandler) {
        if (!nettyHttp2SettingsHandler.subscribeSettingsFrameArrival(this)) {
            settingsFrameArrived = true;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            super.channelRead(ctx, onHttp2HeadersFrame(((Http2HeadersFrame) msg)));
        } else if (msg instanceof Http2DataFrame) {
            super.channelRead(ctx, onHttp2DataFrame(((Http2DataFrame) msg)));
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (settingsFrameArrived) {
            if (msg instanceof Http2Header) {
                super.write(ctx, encodeHttp2HeadersFrame((Http2Header) msg), promise);
            } else if (msg instanceof Http2OutputMessage) {
                super.write(ctx, encodeHttp2DataFrame((Http2OutputMessage) msg), promise);
            } else {
                super.write(ctx, msg, promise);
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Cache writing msg before client connection preface arrival: {}", msg);
        }
        cachedMsgList.add(new CachedMsg(ctx, msg, promise));
    }

    public void notifySettingsFrameArrival() throws Exception {
        if (settingsFrameArrived) {
            return;
        }
        settingsFrameArrived = true;

        if (logger.isDebugEnabled()) {
            logger.debug("Begin cached channel msg writing when client connection preface arrived.");
        }

        for (CachedMsg cached : cachedMsgList) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cached channel msg writing, ctx: {} msg: {}", cached.ctx, cached.msg);
            }
            if (cached.msg instanceof Http2Header) {
                super.write(cached.ctx, encodeHttp2HeadersFrame(((Http2Header) cached.msg)), cached.promise);
            } else if (cached.msg instanceof Http2OutputMessage) {
                super.write(cached.ctx, encodeHttp2DataFrame(((Http2OutputMessage) cached.msg)), cached.promise);
            } else {
                super.write(cached.ctx, cached.msg, cached.promise);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("End cached channel msg writing.");
        }

        cachedMsgList.clear();
    }

    private Http2Header onHttp2HeadersFrame(Http2HeadersFrame headersFrame) {
        return new Http2MetadataFrame(
                headersFrame.stream().id(), new DefaultHttpHeaders(headersFrame.headers()), headersFrame.isEndStream());
    }

    private Http2InputMessage onHttp2DataFrame(Http2DataFrame dataFrame) {
        return new Http2InputMessageFrame(
                dataFrame.stream().id(), new ByteBufInputStream(dataFrame.content(), true), dataFrame.isEndStream());
    }

    @SuppressWarnings("unchecked")
    private Http2HeadersFrame encodeHttp2HeadersFrame(Http2Header http2Header) {
        return new DefaultHttp2HeadersFrame(
                ((NettyHttpHeaders<Http2Headers>) http2Header.headers()).getHeaders(), http2Header.isEndStream());
    }

    private Http2DataFrame encodeHttp2DataFrame(Http2OutputMessage outputMessage) {
        OutputStream body = outputMessage.getBody();
        if (body == null) {
            return new DefaultHttp2DataFrame(outputMessage.isEndStream());
        }
        if (body instanceof ByteBufOutputStream) {
            ByteBuf buffer = ((ByteBufOutputStream) body).buffer();
            return new DefaultHttp2DataFrame(buffer, outputMessage.isEndStream());
        }
        throw new IllegalArgumentException("Http2OutputMessage body must be ByteBufOutputStream");
    }

    private static class CachedMsg {
        private final ChannelHandlerContext ctx;
        private final Object msg;
        private final ChannelPromise promise;

        public CachedMsg(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            this.ctx = ctx;
            this.msg = msg;
            this.promise = promise;
        }
    }
}
