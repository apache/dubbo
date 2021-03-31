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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class UnaryServerStream extends ServerStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnaryServerStream.class);
    private Message message = null;

    public UnaryServerStream(Invoker<?> invoker, ServiceDescriptor serviceDescriptor, MethodDescriptor md,
        ChannelHandlerContext ctx) {
        super(invoker, ExecutorUtil.setThreadName(invoker.getUrl(), "DubboPUServerHandler"), serviceDescriptor, md,
            ctx);
        setProcessor(new Processor(this, getMd(), getUrl(), getSerializeType(), getMultipleSerialization()));
    }

    @Override
    protected void onSingleMessage(InputStream in) {
        if (in == null) {
            responseErr(getCtx(), GrpcStatus.fromCode(Code.INTERNAL)
                .withDescription(MISSING_REQ));
            return;
        }
        message = new Message(getHeaders(), in);
    }

    @Override
    public void streamCreated(Object msg, ChannelPromise promise) {
        Http2HeadersFrame http2HeadersFrame = (Http2HeadersFrame)msg;
        if (http2HeadersFrame.isEndStream()) {
            halfClose();
        }
    }

    @Override
    public void onError(GrpcStatus status) {
    }

    public void halfClose() {
        ExecutorService executor = null;
        if (getProviderModel() != null) {
            executor = (ExecutorService)getProviderModel().getServiceMetadata().getAttribute(
                CommonConstants.THREADPOOL_KEY);
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.getExecutor(getUrl());
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.createExecutorIfAbsent(getUrl());
        }

        try {
            executor.execute(this::unaryInvoke);
        } catch (RejectedExecutionException e) {
            LOGGER.error("Provider's thread pool is full", e);
            responseErr(getCtx(), GrpcStatus.fromCode(Code.RESOURCE_EXHAUSTED)
                .withDescription("Provider's thread pool is full"));
        } catch (Throwable t) {
            LOGGER.error("Provider submit request to thread pool error ", t);
            responseErr(getCtx(), GrpcStatus.fromCode(Code.INTERNAL)
                .withCause(t)
                .withDescription("Provider's error"));
        }
    }

    private void unaryInvoke() {

        RpcInvocation invocation;
        try {
            invocation = buildInvocation();

            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            invocation.setParameterTypes(getMd().getParameterClasses());
            invocation.setReturnTypes(getMd().getReturnTypes());
            InputStream is = message.getIs();
            try {
                if (getProviderModel() != null) {
                    ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
                }

                invocation.setArguments(getProcessor().decodeRequestMessage(is));
            } finally {
                ClassLoadUtil.switchContextLoader(tccl);
            }

        } catch (Throwable t) {
            LOGGER.warn("Exception processing triple message", t);
            responseErr(getCtx(),
                GrpcStatus.fromCode(Code.INTERNAL).withDescription("Decode request failed:" + t.getMessage()));
            return;
        }
        if (invocation == null) {
            return;
        }

        final Result result = getInvoker().invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());

        BiConsumer<Object, Throwable> onComplete = (appResult, t) -> {
            try {
                if (t != null) {
                    if (t instanceof TimeoutException) {
                        responseErr(getCtx(), GrpcStatus.fromCode(Code.DEADLINE_EXCEEDED).withCause(t));
                    } else {
                        responseErr(getCtx(), GrpcStatus.fromCode(Code.UNKNOWN).withCause(t));
                    }
                    return;
                }
                AppResponse response = (AppResponse)appResult;
                if (response.hasException()) {
                    final Throwable exception = response.getException();
                    if (exception instanceof TripleRpcException) {
                        responseErr(getCtx(), ((TripleRpcException)exception).getStatus());
                    } else {
                        responseErr(getCtx(), GrpcStatus.fromCode(Code.UNKNOWN)
                            .withCause(exception));
                    }
                    return;
                }
                Http2Headers http2Headers = new DefaultHttp2Headers()
                    .status(OK.codeAsText())
                    .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                getCtx().write(new DefaultHttp2HeadersFrame(http2Headers));

                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                final ByteBuf buf;
                try {
                    ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
                    buf = getProcessor().encodeResponse(response.getValue(), getCtx());
                } finally {
                    ClassLoadUtil.switchContextLoader(tccl);
                }
                final DefaultHttp2DataFrame data = new DefaultHttp2DataFrame(buf);
                getCtx().write(data);

                final Http2Headers trailers = new DefaultHttp2Headers()
                    .setInt(TripleConstant.STATUS_KEY, Code.OK.code);
                final Map<String, Object> attachments = response.getObjectAttachments();
                if (attachments != null) {
                    convertAttachment(trailers, attachments);
                }
                getCtx().writeAndFlush(new DefaultHttp2HeadersFrame(trailers, true));
            } catch (Throwable e) {
                LOGGER.warn("Exception processing triple message", e);
                if (e instanceof TripleRpcException) {
                    responseErr(getCtx(), ((TripleRpcException)e).getStatus());
                } else {
                    responseErr(getCtx(), GrpcStatus.fromCode(Code.UNKNOWN)
                        .withDescription("Exception occurred in provider's execution:" + e.getMessage())
                        .withCause(e));
                }
            }
        };

        future.whenComplete(onComplete);
    }

}
