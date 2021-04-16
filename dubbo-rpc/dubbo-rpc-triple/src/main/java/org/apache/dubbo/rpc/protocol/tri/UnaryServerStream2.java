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
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.triple.TripleWrapper;

import io.netty.handler.codec.http.HttpHeaderNames;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class UnaryServerStream2 extends ServerStream2 implements Stream {

    protected UnaryServerStream2(URL url) {
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
        public void onComplete(OperationHandler handler) {
            if(getData()==null){
                transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Missing request data"));
                return;
            }
            executorInvoke();
        }


        protected RpcInvocation buildInvocation() {
            RpcInvocation inv = new RpcInvocation();
            inv.setMethodName(getMethodDescriptor().getMethodName());
            inv.setServiceName(getServiceDescriptor().getServiceName());
            inv.setTargetServiceUniqueName(getUrl().getServiceKey());
            final Map<String, Object> attachments = parseMetadataToMap(getHeaders());
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

        public void invoke() {

            final RpcInvocation invocation;
            try {
                invocation = buildInvocation();

                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                invocation.setParameterTypes(getMethodDescriptor().getParameterClasses());
                invocation.setReturnTypes(getMethodDescriptor().getReturnTypes());
                byte[] data = getData();
                try {
                    if (getProviderModel() != null) {
                        ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
                    }
                    if (getMethodDescriptor().isNeedWrap()) {
                        final TripleWrapper.TripleRequestWrapper wrapper = TripleUtil.unpack(data, TripleWrapper.TripleRequestWrapper.class);
                        setSerializeType(wrapper.getSerializeType());
                        invocation.setArguments(TripleUtil.unwrapReq(getUrl(), wrapper, getMultipleSerialization()));
                    } else {
                        invocation.setArguments(new Object[]{TripleUtil.unpack(data, getMethodDescriptor().getParameterClasses()[0])});
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
                    Metadata metadata = new DefaultMetadata();
                    metadata.put(TripleConstant.HTTP_STATUS_KEY, Integer.toString(OK.code()))
                            .put(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                    getTransportSubscriber().tryOnMetadata(metadata, false);

                    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                    final byte[] data;
                    try {
                        ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
                        data = encodeResponse(response.getValue());
                    } finally {
                        ClassLoadUtil.switchContextLoader(tccl);
                    }
                    getTransportSubscriber().tryOnData(data, false);

                    Metadata trailers = new DefaultMetadata()
                            .put(TripleConstant.STATUS_KEY, Integer.toString(GrpcStatus.Code.OK.code));
                    final Map<String, Object> attachments = response.getObjectAttachments();
                    if (attachments != null) {
                        convertAttachment(trailers, attachments);
                    }
                    getTransportSubscriber().tryOnMetadata(trailers, true);
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
