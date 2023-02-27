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
package org.apache.dubbo.rpc.protocol.rest.netty;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;


import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


public class NettyHttpResponse implements HttpResponse {
    private static final int EMPTY_CONTENT_LENGTH = 0;
    private int status = 200;
    private OutputStream os;
    private Map<String, Object> outputHeaders;
    private final ChannelHandlerContext ctx;
    private boolean committed;
    private boolean keepAlive;
    private HttpMethod method;

    public NettyHttpResponse(final ChannelHandlerContext ctx, final boolean keepAlive) {
        this(ctx, keepAlive, null);
    }

    public NettyHttpResponse(final ChannelHandlerContext ctx, final boolean keepAlive, final HttpMethod method) {
        outputHeaders = new HashMap<>();
        this.method = method;
        os = (method == null || !method.equals(HttpMethod.HEAD)) ? new ChunkOutputStream(this, ctx, 1000) : null; //[RESTEASY-1627]
        this.ctx = ctx;
        this.keepAlive = keepAlive;
    }


    public void setOutputStream(OutputStream os) {
        this.os = os;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public Map<String, Object> getOutputHeaders() {
        return outputHeaders;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return os;
    }



    @Override
    public void sendError(int status) throws IOException {
        sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        if (committed) {
            throw new IllegalStateException();
        }

        final HttpResponseStatus responseStatus;
        if (message != null) {
            responseStatus = new HttpResponseStatus(status, message);
            setStatus(status);
        } else {
            responseStatus = HttpResponseStatus.valueOf(status);
            setStatus(status);
        }
        io.netty.handler.codec.http.HttpResponse response = null;
        if (message != null) {
            ByteBuf byteBuf = ctx.alloc().buffer();
            byteBuf.writeBytes(message.getBytes());

            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, byteBuf);
        } else {
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus);

        }
        if (keepAlive) {
            // Add keep alive and content length if needed
            response.headers().add(Names.CONNECTION, Values.KEEP_ALIVE);
            if (message == null) response.headers().add(Names.CONTENT_LENGTH, 0);
            else response.headers().add(Names.CONTENT_LENGTH, message.getBytes().length);
        }
        ctx.writeAndFlush(response);
        committed = true;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void reset() {
        if (committed) {
            throw new IllegalStateException("Messages.MESSAGES.alreadyCommitted()");
        }
        outputHeaders.clear();
        outputHeaders.clear();
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public DefaultHttpResponse getDefaultHttpResponse() {
        DefaultHttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(getStatus()));

        return res;
    }

    public DefaultHttpResponse getEmptyHttpResponse() {
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(getStatus()));
        if (method == null || !method.equals(HttpMethod.HEAD)) //[RESTEASY-1627]
        {
            res.headers().add(Names.CONTENT_LENGTH, EMPTY_CONTENT_LENGTH);
        }

        return res;
    }



    public void prepareChunkStream() {
        committed = true;
        DefaultHttpResponse response = getDefaultHttpResponse();
        HttpHeaders.setTransferEncodingChunked(response);
        ctx.write(response);
    }

    public void finish() throws IOException {
        if (os != null)
            os.flush();
        ChannelFuture future;
        if (isCommitted()) {
            // if committed this means the output stream was used.
            future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            future = ctx.writeAndFlush(getEmptyHttpResponse());
        }

        if (!isKeepAlive()) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void flushBuffer() throws IOException {
        if (os != null)
            os.flush();
        ctx.flush();
    }
}
