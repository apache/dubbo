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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2NoMoreStreamIdsException;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AsciiString;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;

public class ClientStream extends AbstractStream implements Stream {

    public static ClientStream unary(){

    }
    private static final AsciiString SCHEME = AsciiString.of("http");
    private final String authority;
    private final Request request;
    private final RpcInvocation invocation;
    private final Executor callback;
    private Http2StreamChannel streamChannel;
    private Message message;

    public ClientStream(URL url, ChannelHandlerContext ctx, MethodDescriptor md, Request request, Executor callback) {
        super(url, ctx, md);

        if (md.isNeedWrap()) {
            setSerializeType(
                (String)((RpcInvocation)(request.getData())).getObjectAttachment(Constants.SERIALIZATION_KEY));
        }
        this.callback = callback;
        this.authority = url.getAddress();
        this.request = request;
        this.invocation = (RpcInvocation)request.getData();
        setProcessor(new Processor(this, getMd(), getUrl(), getSerializeType(), getMultipleSerialization()));
    }


    public Executor getCallback() {
        return callback;
    }

    public Request getRequest() {
        return request;
    }

    public RpcInvocation getInvocation() {
        return invocation;
    }

    public Http2StreamChannel getStreamChannel() {
        return streamChannel;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void onError(GrpcStatus status) {
        Response response = new Response(request.getId(), request.getVersion());
        if (status.description != null) {
            response.setErrorMessage(status.description);
        } else {
            response.setErrorMessage(status.cause.getMessage());
        }
        final byte code = GrpcStatus.toDubboStatus(status.code);
        response.setStatus(code);
        DefaultFuture2.received(Connection.getConnectionFromChannel(getCtx().channel()), response);
    }

    @Override
    public void streamCreated(Object msg, ChannelPromise promise) {

        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(getCtx().channel());
        streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();
        TripleUtil.setClientStream(streamChannel, this);


    }

    @Override
    public void onNext(Object data) {
        write(data, null);
    }

    @Override
    protected void onSingleMessage(InputStream in) {
        if (getMd().isStream()) {
            getProcessor().onSingleResponseMessage(in);
        } else {
            message = new Message(getHeaders(), in);
        }
    }

    public void onCompleted() {
        if (getCanceled().compareAndSet(false, true)) {
            streamChannel.writeAndFlush(new DefaultHttp2DataFrame(true));
        }
    }
}
