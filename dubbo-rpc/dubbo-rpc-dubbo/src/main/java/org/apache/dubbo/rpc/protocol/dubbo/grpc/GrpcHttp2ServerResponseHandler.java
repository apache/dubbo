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
package org.apache.dubbo.rpc.protocol.dubbo.grpc;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Http2Packet;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.netty4.DubboHttp2ConnectionHandler;
import org.apache.dubbo.remoting.netty4.StreamData;
import org.apache.dubbo.remoting.netty4.StreamHeader;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * NettyServerHandler.
 */
public class GrpcHttp2ServerResponseHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(GrpcHttp2ServerResponseHandler.class);


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);

    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        if (msg instanceof Http2Packet) {
            Http2Packet packet = (Http2Packet)msg;
            int streamId = packet.getStreamId();
            if (packet.getStatus() == Response.OK) {
                Http2Headers http2Headers = new DefaultHttp2Headers()
                    .status(OK.codeAsText())
                    .set(HttpHeaderNames.CONTENT_TYPE, GrpcElf.GRPC_PROTO);
                StreamHeader streamHeader = new StreamHeader(streamId, http2Headers, false);
                ctx.channel().write(streamHeader);

                Object res = packet.getResult();
                ByteBuf data = Marshaller.marshaller.marshaller(ctx.alloc(), res);
                StreamData streamData = new StreamData(true, streamId, data);
                ctx.channel().write(streamData);
            }
        }

    }


}
