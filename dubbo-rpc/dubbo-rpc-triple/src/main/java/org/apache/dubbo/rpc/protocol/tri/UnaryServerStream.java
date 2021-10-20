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
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import io.netty.handler.codec.http.HttpHeaderNames;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.apache.dubbo.rpc.protocol.tri.GrpcStatus.rpcExceptionCodeToGrpc;

public class UnaryServerStream extends AbstractServerStream implements Stream {

    protected UnaryServerStream(URL url) {
        super(url);
    }

    @Override
    protected StreamObserver<Object> createStreamObserver() {
        return null;
    }

    @Override
    protected TransportObserver createTransportObserver() {
        return new UnaryServerTransportObserver();
    }

    private class UnaryServerTransportObserver extends UnaryTransportObserver implements TransportObserver {
        @Override
        protected void onError(GrpcStatus status) {
            transportError(status);
        }

        @Override
        public void doOnComplete() {
            if (getData() != null) {
                execute(this::invoke);
            } else {
                onError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Missing request data"));
            }
        }

        public void invoke() {

            RpcInvocation invocation;
            try {
                invocation = buildInvocation(getHeaders());
                final Object[] arguments = deserializeRequest(getData());
                if (arguments != null) {
                    invocation.setArguments(arguments);
                } else {
                    return;
                }
            } catch (Throwable t) {
                LOGGER.warn("Exception processing triple message", t);
                transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Decode request failed:" + t.getMessage()));
                return;
            }

            final Result result = getInvoker().invoke(invocation);
            CompletionStage<Object> future = result.thenApply(Function.identity());

            BiConsumer<Object, Throwable> onComplete = (appResult, t) -> {
                if (t != null) {
                    if (t instanceof TimeoutException) {
                        transportError(GrpcStatus.fromCode(GrpcStatus.Code.DEADLINE_EXCEEDED).withCause(t));
                    } else {
                        transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withCause(t));
                    }
                    return;
                }
                AppResponse response = (AppResponse) appResult;
                try {
                    if (response.hasException()) {
                        final Throwable exception = response.getException();
                        if (exception instanceof RpcException) {
                            transportError(rpcExceptionCodeToGrpc(((RpcException) exception).getCode())
                                .withCause(exception), response.getObjectAttachments());
                            final GrpcStatus status = rpcExceptionCodeToGrpc(((RpcException) exception).getCode())
                                .withCause(exception);
                            transportError(status, response.getObjectAttachments());
                        } else {
                            transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                                .withCause(exception), response.getObjectAttachments());
                        }
                        return;
                    }
                    Metadata metadata = createRequestMeta();
                    metadata.put(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                    getTransportSubscriber().onMetadata(metadata, false);

                    final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                    final byte[] data;
                    try {
                        ClassLoadUtil.switchContextLoader(
                            getProviderModel().getServiceInterfaceClass().getClassLoader());
                        data = encodeResponse(response.getValue());
                    } finally {
                        ClassLoadUtil.switchContextLoader(tccl);
                    }
                    getTransportSubscriber().onData(data, false);

                    Metadata trailers = new DefaultMetadata()
                        .put(TripleHeaderEnum.STATUS_KEY.getHeader(), Integer.toString(GrpcStatus.Code.OK.code));
                    final Map<String, Object> attachments = response.getObjectAttachments();
                    if (attachments != null) {
                        convertAttachment(trailers, attachments);
                    }
                    getTransportSubscriber().onMetadata(trailers, true);
                } catch (Throwable e) {
                    LOGGER.warn("Exception processing triple message", e);
                    if (e instanceof RpcException) {
                        final GrpcStatus status = rpcExceptionCodeToGrpc(((RpcException) e).getCode())
                            .withCause(e);
                        transportError(status, response.getObjectAttachments());
                    } else {
                        transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                            .withDescription("Exception occurred in provider's execution:" + e.getMessage())
                            .withCause(e), response.getObjectAttachments());
                    }
                }
            };

            future.whenComplete(onComplete);
            RpcContext.removeContext();
        }
    }

}
