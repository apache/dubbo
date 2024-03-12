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
package org.apache.dubbo.remoting.http3.netty4;

import io.netty.channel.ChannelHandlerContext;

import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http3.h3.Http3TransportListener;

public class NettyHttp3FrameHandler extends NettyHttp3StreamInboundHandler {
    private final Http3TransportListener transportListener;
    private State state = State.NONE;

    public NettyHttp3FrameHandler(Http3TransportListener transportListener) {
        this.transportListener = transportListener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2Header) {
            state = State.HEADER;
            transportListener.onMetadata((Http2Header) msg);
        } else if (msg instanceof Http2InputMessage) {
            state = State.DATA;
            transportListener.onData((Http2InputMessage) msg);
        } else {
            state = State.OTHER;
            super.channelRead(ctx, msg);
        }
    }

    @Override
    protected void channelEndStream(ChannelHandlerContext ctx) throws Exception {
        if (state != State.NONE) {
            transportListener.onDataCompletion();
        }
    }

    // todo: userEventTriggered cancel

    private enum State {
        /**
         * Received no frame yet
         */
        NONE,
        /**
         * The last frame received is HEADERS
         */
        HEADER,
        /**
         * The last frame received is DATA
         */
        DATA,
        /**
         * The last frame received is another type
         */
        OTHER
    }
}
