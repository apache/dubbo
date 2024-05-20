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
package org.apache.dubbo.rpc.protocol.tri.h12;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.InboundHttpToHttp2Adapter;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * If an upgrade occurred, the program need send a simple response via HTTP/2 on stream 1 (the stream specifically reserved
 * for cleartext HTTP upgrade). However, {@link Http2FrameCodec} send 'upgradeRequest' to upgraded channel handlers by
 * {@link InboundHttpToHttp2Adapter} (As it noted that this may behave strangely). So we need to distinguish the 'upgradeRequest'
 * and send the response.<br/>
 *
 * @see HttpServerUpgradeHandler
 * @see Http2FrameCodec
 * @see InboundHttpToHttp2Adapter
 * @since 3.3.0
 */
@Sharable
public class HttpServerAfterUpgradeHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttp2HeadersFrame) {
            DefaultHttp2HeadersFrame headersFrame = (DefaultHttp2HeadersFrame) msg;
            if (headersFrame.stream().id() == Http2CodecUtil.HTTP_UPGRADE_STREAM_ID && headersFrame.isEndStream()) {
                // upgradeRequest
                sendResponse(ctx, headersFrame.stream());
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    /**
     * Send a frame for the response status
     */
    private static void sendResponse(ChannelHandlerContext ctx, Http2FrameStream stream) {
        Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
        ctx.write(new DefaultHttp2HeadersFrame(headers).stream(stream));
        ctx.write(new DefaultHttp2DataFrame(true).stream(stream));
    }
}
