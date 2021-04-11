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

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2NoMoreStreamIdsException;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AsciiString;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import static org.apache.dubbo.rpc.Constants.CONSUMER_MODEL;

public class UnaryClientStream extends ClientStream {
    private static final GrpcStatus MISSING_RESP = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
        .withDescription("Missing Response");

    public UnaryClientStream(URL url, ChannelHandlerContext ctx, MethodDescriptor md, Request request, Executor callback) {
        super(url, ctx, md, request, callback);
    }

    public static ConsumerModel getConsumerModel(Invocation invocation) {
        Object o = invocation.get(CONSUMER_MODEL);
        if (o instanceof ConsumerModel) {
            return (ConsumerModel)o;
        }
        String serviceKey = invocation.getInvoker().getUrl().getServiceKey();
        return ApplicationModel.getConsumerModel(serviceKey);
    }

    public void halfClose() {
        final int httpCode = HttpResponseStatus.parseLine(getHeaders().status()).code();
        if (HttpResponseStatus.OK.code() != httpCode) {
            final Integer code = getHeaders().getInt(TripleConstant.STATUS_KEY);
            final GrpcStatus status = GrpcStatus.fromCode(code)
                .withDescription(TripleUtil.percentDecode(getHeaders().get(TripleConstant.MESSAGE_KEY)));
            onError(status);
            return;
        }
        final Http2Headers te = getTe() == null ? getHeaders() : getTe();
        final Integer code = te.getInt(TripleConstant.STATUS_KEY);
        if (!GrpcStatus.Code.isOk(code)) {
            final GrpcStatus status = GrpcStatus.fromCode(code)
                .withDescription(TripleUtil.percentDecode(getHeaders().get(TripleConstant.MESSAGE_KEY)));
            onError(status);
            return;
        }
        final InputStream data = getMessage().getIs();
        if (data == null) {
            onError(MISSING_RESP);
            return;
        }

        getCallback().execute(() -> {
            final Invocation invocation = (Invocation) (getRequest().getData());
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                final Object resp;
                final ConsumerModel model = getConsumerModel(invocation);
                if (model != null) {
                    ClassLoadUtil.switchContextLoader(model.getClassLoader());
                }
                resp = getProcessor().decodeResponseMessage(data);
                Response response = new Response(getRequest().getId(), getRequest().getVersion());
                final AppResponse result = new AppResponse(resp);
                result.setObjectAttachments(parseHeadersToMap(te));
                response.setResult(result);
                DefaultFuture2.received(Connection.getConnectionFromChannel(getCtx().channel()), response);
            } catch (Exception e) {
                final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withCause(e)
                        .withDescription("Failed to deserialize response");
                onError(status);
            } finally {
                ClassLoadUtil.switchContextLoader(tccl);
            }
        });
    }

    @Override
    public void write(Object obj, ChannelPromise promise) {
        final ByteBuf out;

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            final ConsumerModel model = getConsumerModel(getInvocation());
            if (model != null) {
                ClassLoadUtil.switchContextLoader(model.getClassLoader());
            }
            out = getProcessor().encodeRequest(getInvocation(), getCtx());
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
        final DefaultHttp2DataFrame data = new DefaultHttp2DataFrame(out, true);
        getStreamChannel().write(data).addListener(f -> {
            if (f.isSuccess()) {
                promise.trySuccess();
            } else {
                promise.tryFailure(f.cause());
            }
        });
    }
}
