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
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.triple.TripleWrapper;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.dubbo.rpc.protocol.tri.ServerStream.EXECUTOR_REPOSITORY;

public class UnaryServerStream2 extends ServerStream2 implements Stream {
    private InputStream data;

    protected UnaryServerStream2(URL url) {
        super(url);
    }

    @Override
    protected void appendData(InputStream in, OperationHandler handler) {
        if (data != null) {
            this.data = in;
        } else {
            handler.operationDone(OperationResult.FAILURE,
                    GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL).withDescription(MISSING_REQ).asException());
        }
    }

    @Override
    public void onComplete(OperationHandler handler) {
        invoke();
    }

    private class UnaryServerTransportObserver extends UnaryTransportObserver implements TransportObserver {
        @Override
        public void onComplete(OperationHandler handler) {
            ExecutorService executor = null;
            if (getProviderModel() != null) {
                executor = (ExecutorService) getProviderModel().getServiceMetadata().getAttribute(
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
                transportError(GrpcStatus.fromCode(GrpcStatus.Code.RESOURCE_EXHAUSTED)
                        .withDescription("Provider's thread pool is full"));
            } catch (Throwable t) {
                LOGGER.error("Provider submit request to thread pool error ", t);
                transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withCause(t)
                        .withDescription("Provider's error"));
            }
        }


        protected RpcInvocation buildInvocation() {
            RpcInvocation inv = new RpcInvocation();
            inv.setMethodName(getMethodDescriptor().getMethodName());
            inv.setServiceName(getServiceDescriptor().getServiceName());
            inv.setTargetServiceUniqueName(getUrl().getServiceKey());
            final Map<String, Object> attachments = parseHeadersToMap(getHeaders());
            attachments.remove("interface");
            attachments.remove("serialization");
            attachments.remove("te");
            attachments.remove("path");
            attachments.remove(TripleConstant.CONTENT_TYPE_KEY);
            attachments.remove(TripleConstant.SERVICE_GROUP);
            attachments.remove(TripleConstant.SERVICE_VERSION);
            attachments.remove(TripleConstant.MESSAGE_KEY);
            attachments.remove(TripleConstant.STATUS_KEY);
            attachments.remove(TripleConstant.TIMEOUT);
            inv.setObjectAttachments(attachments);
            return inv;
        }

        private void unaryInvoke() {

            final RpcInvocation invocation ;
            try {
                invocation = buildInvocation();

                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                invocation.setParameterTypes(getMethodDescriptor().getParameterClasses());
                invocation.setReturnTypes(getMethodDescriptor().getReturnTypes());
                InputStream is = getData();
                try {
                    if (getProviderModel() != null) {
                        ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
                    }
                    if (getMethodDescriptor().isNeedWrap()) {
                        final TripleWrapper.TripleRequestWrapper wrapper = TripleUtil.unpack(is, TripleWrapper.TripleRequestWrapper.class);
                        invocation.setArguments(TripleUtil.unwrapReq(getUrl(), wrapper, getMultipleSerialization()));
                    } else {
                        invocation.setArguments(new Object[]{TripleUtil.unpack(is, getMethodDescriptor().getParameterClasses()[0])});
                    }

                } finally {
                    ClassLoadUtil.switchContextLoader(tccl);
                }

            } catch (Throwable t) {
                LOGGER.warn("Exception processing triple message", t);
                transportError(
                        GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL).withDescription("Decode request failed:" + t.getMessage()));
                return;
            }

            final Result result = getInvoker().invoke(invocation);
            CompletionStage<Object> future = result.thenApply(Function.identity());

            BiConsumer<Object, Throwable> onComplete = (appResult, t) -> {
                try {
                    if (t != null) {
                        if (t instanceof TimeoutException) {
                            transportError(GrpcStatus.fromCode(GrpcStatus.Code.DEADLINE_EXCEEDED).withCause(t));
                        } else {
                            transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withCause(t));
                        }
                        return;
                    }
                    AppResponse response = (AppResponse) appResult;
                    if (response.hasException()) {
                        final Throwable exception = response.getException();
                        if (exception instanceof TripleRpcException) {
                            transportError(((TripleRpcException) exception).getStatus());
                        } else {
                            transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                                    .withCause(exception));
                        }
                        return;
                    }
                    Metadata metadata=new Metadata();
                    metadata.put(TripleConstant.HTTP_STATUS_KEY,Integer.toString(OK.code()))
                    .put(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                    getTransportSubscriber().onMetadata(metadata,false,null);

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
                            .setInt(TripleConstant.STATUS_KEY, GrpcStatus.Code.OK.code);
                    final Map<String, Object> attachments = response.getObjectAttachments();
                    if (attachments != null) {
                        convertAttachment(trailers, attachments);
                    }
                    getCtx().writeAndFlush(new DefaultHttp2HeadersFrame(trailers, true));
                } catch (Throwable e) {
                    LOGGER.warn("Exception processing triple message", e);
                    if (e instanceof TripleRpcException) {
                        transportError(((TripleRpcException) e).getStatus());
                    } else {
                        transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                                .withDescription("Exception occurred in provider's execution:" + e.getMessage())
                                .withCause(e));
                    }
                }
            };

            future.whenComplete(onComplete);
            RpcContext.removeContext();
        }
    }


}
