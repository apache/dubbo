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


import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
 * netty http response
 */
public class NettyHttpResponse implements HttpResponse {
    private static final int EMPTY_CONTENT_LENGTH = 0;
    private int status = 200;
    private OutputStream os;
    private Map<String, List<String>> outputHeaders;
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
        // TODO chunk size to config
        os = new ChunkOutputStream(this, ctx, 1000);
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
        if (status > 200) {
            addOutputHeaders(RestHeaderEnum.CONTENT_TYPE.getHeader(), MediaType.TEXT_PLAIN.value);
        }
        this.status = status;
    }

    @Override
    public Map<String, List<String>> getOutputHeaders() {
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
        setStatus(status);
        if (message != null) {
            getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        }

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
        transformResponseHeaders(res);
        return res;
    }

    public DefaultHttpResponse getEmptyHttpResponse() {
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(getStatus()));
        if (method == null || !method.equals(HttpMethod.HEAD)) {
            res.headers().add(Names.CONTENT_LENGTH, EMPTY_CONTENT_LENGTH);
        }
        transformResponseHeaders(res);

        return res;
    }

    private void transformResponseHeaders(io.netty.handler.codec.http.HttpResponse res) {
        transformHeaders(this, res);
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

        getOutputStream().close();
    }

    @Override
    public void flushBuffer() throws IOException {
        if (os != null)
            os.flush();
        ctx.flush();
    }

    @Override
    public void addOutputHeaders(String name, String value) {

        List<String> values = outputHeaders.get(name);

        if (values == null) {
            values = new ArrayList<>();
            outputHeaders.put(name, values);
        }

        values.add(value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void transformHeaders(NettyHttpResponse nettyResponse, io.netty.handler.codec.http.HttpResponse response) {
//        if (nettyResponse.isKeepAlive()) {
//            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
//        } else {
//            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
//        }

        for (Map.Entry<String, List<String>> entry : nettyResponse.getOutputHeaders().entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                response.headers().set(key, value);
            }
        }

    }
}
