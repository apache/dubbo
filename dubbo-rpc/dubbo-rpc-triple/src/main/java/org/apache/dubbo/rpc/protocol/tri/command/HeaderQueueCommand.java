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

import org.apache.dubbo.rpc.protocol.tri.Metadata;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;

public class HeaderQueueCommand extends QueuedCommand.AbstractQueuedCommand {

    private final Http2Headers headers;

    private final boolean endStream;

    private HeaderQueueCommand(Metadata headers, boolean endStream) {
        this(getHttp2Headers(headers), endStream);
    }

    private HeaderQueueCommand(Http2Headers headers, boolean endStream) {
        this.headers = headers;
        this.endStream = endStream;
    }

    public static HeaderQueueCommand createHeaders(Metadata headers, boolean endStream) {
        return new HeaderQueueCommand(getHttp2Headers(headers), endStream);
    }

    public static HeaderQueueCommand createHeaders(Metadata headers) {
        return new HeaderQueueCommand(headers, false);
    }

    public static HeaderQueueCommand createHeaders(Http2Headers headers) {
        return new HeaderQueueCommand(headers, false);
    }

    public static HeaderQueueCommand createHeaders(Http2Headers headers, boolean endStream) {
        return new HeaderQueueCommand(headers, endStream);
    }

    public static HeaderQueueCommand createTrailers(Metadata headers) {
        return new HeaderQueueCommand(headers, true);
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public boolean isEndStream() {
        return endStream;
    }

    private static Http2Headers getHttp2Headers(Metadata metadata) {
        Http2Headers http2Headers = new DefaultHttp2Headers(true);
        metadata.forEach((kv) -> http2Headers.set(kv.getKey(), kv.getValue()));
        return http2Headers;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.write(new DefaultHttp2HeadersFrame(headers, endStream), promise);
    }
}
