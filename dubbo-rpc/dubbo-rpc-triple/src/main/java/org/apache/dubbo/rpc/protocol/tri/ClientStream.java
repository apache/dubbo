/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
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
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.triple.TripleWrapper;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.apache.dubbo.rpc.Constants.CONSUMER_MODEL;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class ClientStream extends AbstractStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStream.class);
    private static final GrpcStatus MISSING_RESP = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("Missing Response");
    private final Request request;
    private final RpcInvocation invocation;


    public ClientStream(URL url, ChannelHandlerContext ctx, boolean needWrap, Request request) {
        super(url, ctx, needWrap);
        if (needWrap) {
            setSerializeType((String) ((RpcInvocation) (request.getData())).getObjectAttachment(Constants.SERIALIZATION_KEY));
        }
        this.request = request;
        this.invocation = (RpcInvocation) request.getData();
    }

    public static ConsumerModel getConsumerModel(Invocation invocation) {
        Object o = invocation.get(CONSUMER_MODEL);
        if (o instanceof ConsumerModel) {
            return (ConsumerModel) o;
        }
        String serviceKey = invocation.getInvoker().getUrl().getServiceKey();
        return ApplicationModel.getConsumerModel(serviceKey);
    }

    @Override
    public void onError(GrpcStatus status) {
        Response response = new Response(request.getId(), request.getVersion());
        if (status.description != null) {
            response.setErrorMessage(status.description);
        } else {
            response.setErrorMessage(status.cause.getMessage());
        }
        // TODO map grpc status to response status
        response.setStatus(Response.BAD_REQUEST);
        DefaultFuture2.received(Connection.getConnectionFromChannel(getCtx().channel()), response);
    }

    @Override
    public void write(Object obj, ChannelPromise promise) throws IOException {
        Http2Headers headers = new DefaultHttp2Headers()
                .method(HttpMethod.POST.asciiName())
                .path("/" + invocation.getObjectAttachment(CommonConstants.PATH_KEY) + "/" + invocation.getMethodName())
                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);

        final String version = (String) invocation.getObjectAttachment(CommonConstants.VERSION_KEY);
        if (version != null) {
            headers.set(TripleConstant.SERVICE_VERSION, version);
            invocation.getObjectAttachments().remove(CommonConstants.VERSION_KEY);
        }

        final String app = (String) invocation.getObjectAttachment(CommonConstants.APPLICATION_KEY);
        if (app != null) {
            headers.set(TripleConstant.CONSUMER_APP_NAME_KEY, app);
            invocation.getObjectAttachments().remove(CommonConstants.APPLICATION_KEY);
        }

        final String group = (String) invocation.getObjectAttachment(CommonConstants.GROUP_KEY);
        if (group != null) {
            headers.set(TripleConstant.SERVICE_GROUP, group);
            invocation.getObjectAttachments().remove(CommonConstants.GROUP_KEY);
        }
        final Map<String, Object> attachments = invocation.getObjectAttachments();
        if (attachments != null) {
            convertAttachment(headers, attachments);
        }
        DefaultHttp2HeadersFrame frame = new DefaultHttp2HeadersFrame(headers);
        final TripleHttp2ClientResponseHandler responseHandler = new TripleHttp2ClientResponseHandler();

        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(getCtx().channel());
        final Http2StreamChannel streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();
        TripleUtil.setClientStream(streamChannel, this);
        streamChannel.pipeline().addLast(responseHandler)
                .addLast(new GrpcDataDecoder(Integer.MAX_VALUE))
                .addLast(new TripleClientInboundHandler());
        streamChannel.write(frame).addListener(future -> {
            if (!future.isSuccess()) {
                if (future.cause() instanceof Http2NoMoreStreamIdsException) {
                    getCtx().close();
                }
                promise.setFailure(future.cause());
            }
        });
        final ByteBuf out;

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            final ConsumerModel model = getConsumerModel(invocation);
            if (model != null) {
                ClassLoadUtil.switchContextLoader(model.getClassLoader());
            }
            if (isNeedWrap()) {
                final TripleWrapper.TripleRequestWrapper wrap = TripleUtil.wrapReq(getUrl(), invocation, getMultipleSerialization());
                out = TripleUtil.pack(getCtx(), wrap);
            } else {
                out = TripleUtil.pack(getCtx(), invocation.getArguments()[0]);
            }
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
        streamChannel.write(new DefaultHttp2DataFrame(out, true));

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
        Http2Headers te = getTe();
        if (te == null) {
            te = getHeaders();
        }
        final Integer code = te.getInt(TripleConstant.STATUS_KEY);
        if (!GrpcStatus.Code.isOk(code)) {
            final GrpcStatus status = GrpcStatus.fromCode(code)
                    .withDescription(TripleUtil.percentDecode(getHeaders().get(TripleConstant.MESSAGE_KEY)));
            onError(status);
            return;
        }
        final InputStream data = getData();
        if (data == null) {
            responseErr(getCtx(), MISSING_RESP);
            return;
        }
        final Invocation invocation = (Invocation) (request.getData());
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(invocation.getServiceName(), invocation.getMethodName());
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            final Object resp;
            final ConsumerModel model = getConsumerModel(invocation);
            if (model != null) {
                ClassLoadUtil.switchContextLoader(model.getClassLoader());
            }
            if (isNeedWrap()) {
                final TripleWrapper.TripleResponseWrapper message = TripleUtil.unpack(data, TripleWrapper.TripleResponseWrapper.class);
                resp = TripleUtil.unwrapResp(getUrl(), message, getMultipleSerialization());
            } else {
                resp = TripleUtil.unpack(data, methodDescriptor.getReturnClass());
            }
            Response response = new Response(request.getId(), request.getVersion());
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
    }

}
