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

package org.apache.dubbo.rpc.protocol.tri.command;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

public class HeaderQueueCommand extends StreamQueueCommand {

    private final Http2Headers headers;

    private final boolean endStream;

    private HeaderQueueCommand(TripleStreamChannelFuture streamChannelFuture, Http2Headers headers, boolean endStream) {
        super(streamChannelFuture);
        this.headers = headers;
        this.endStream = endStream;
    }

    public static HeaderQueueCommand createHeaders(TripleStreamChannelFuture streamChannelFuture, Http2Headers headers) {
        return new HeaderQueueCommand(streamChannelFuture, headers, false);
    }

    public static HeaderQueueCommand createHeaders(TripleStreamChannelFuture streamChannelFuture, Http2Headers headers, boolean endStream) {
        return new HeaderQueueCommand(streamChannelFuture, headers, endStream);
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public boolean isEndStream() {
        return endStream;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.write(new DefaultHttp2HeadersFrame(headers, endStream), promise);
    }
}
