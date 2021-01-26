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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class ServerStream extends AbstractStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStream.class);
    private static final String TOO_MANY_REQ = "Too many requests";
    private static final String MISSING_REQ = "Missing request";
    private static final ExecutorRepository EXECUTOR_REPOSITORY =
            ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
    private final Invoker<?> invoker;
    private final ChannelHandlerContext ctx;
    private final ServiceDescriptor serviceDescriptor;
    private final ProviderModel providerModel;
    private final String methodName;
    private MethodDescriptor methodDescriptor;


    public ServerStream(Invoker<?> invoker, ServiceDescriptor serviceDescriptor, String methodName, ChannelHandlerContext ctx) {
        super(ExecutorUtil.setThreadName(invoker.getUrl(), "DubboPUServerHandler"), ctx);
        this.invoker = invoker;
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        this.providerModel = repo.lookupExportedService(getUrl().getServiceKey());
        this.methodName = methodName;
        this.serviceDescriptor = serviceDescriptor;
        this.ctx = ctx;
    }


    @Override
    public void onError(GrpcStatus status) {
    }

    @Override
    public void write(Object obj, ChannelPromise promise) throws Exception {

    }

    public void halfClose() throws Exception {
        if (getData() == null) {
            responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription(MISSING_REQ));
            return;
        }
        ExecutorService executor = null;
        if (providerModel != null) {
            executor = (ExecutorService) providerModel.getServiceMetadata().getAttribute(CommonConstants.THREADPOOL_KEY);
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
            responseErr(ctx, GrpcStatus.fromCode(Code.RESOURCE_EXHAUSTED)
                    .withDescription("Provider's thread pool is full"));
        } catch (Throwable t) {
            LOGGER.error("Provider submit request to thread pool error ", t);
            responseErr(ctx, GrpcStatus.fromCode(Code.INTERNAL)
                    .withCause(t)
                    .withDescription("Provider's error"));
        }
    }

    private void unaryInvoke() {

        Invocation invocation;
        try {
            invocation = buildInvocation();
        } catch (Throwable t) {
            LOGGER.warn("Exception processing triple message", t);
            responseErr(ctx, GrpcStatus.fromCode(Code.INTERNAL).withDescription("Decode request failed:" + t.getMessage()));
            return;
        }
        if (invocation == null) {
            return;
        }

        final Result result = this.invoker.invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());

        BiConsumer<Object, Throwable> onComplete = (appResult, t) -> {
            try {
                if (t != null) {
                    if (t instanceof TimeoutException) {
                        responseErr(ctx, GrpcStatus.fromCode(Code.DEADLINE_EXCEEDED).withCause(t));
                    } else {
                        responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withCause(t));
                    }
                    return;
                }
                AppResponse response = (AppResponse) appResult;
                if (response.hasException()) {
                    final Throwable exception = response.getException();
                    if (exception instanceof TripleRpcException) {
                        responseErr(ctx, ((TripleRpcException) exception).getStatus());
                    } else {
                        responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                                .withCause(exception));
                    }
                    return;
                }
                Http2Headers http2Headers = new DefaultHttp2Headers()
                        .status(OK.codeAsText())
                        .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                final Message message;

                ClassLoader tccl = Thread.currentThread().getContextClassLoader();

                final ByteBuf buf;
                try {
                    ClassLoadUtil.switchContextLoader(providerModel.getServiceInterfaceClass().getClassLoader());
                    if (isNeedWrap()) {
                        message = TripleUtil.wrapResp(getUrl(), getSerializeType(), response.getValue(), methodDescriptor, getMultipleSerialization());
                    } else {
                        message = (Message) response.getValue();
                    }
                    buf = TripleUtil.pack(ctx, message);
                } finally {
                    ClassLoadUtil.switchContextLoader(tccl);
                }

                final Http2Headers trailers = new DefaultHttp2Headers()
                        .setInt(TripleConstant.STATUS_KEY, GrpcStatus.Code.OK.code);
                final Map<String, Object> attachments = response.getObjectAttachments();
                if (attachments != null) {
                    convertAttachment(trailers, attachments);
                }
                ctx.write(new DefaultHttp2HeadersFrame(http2Headers));
                ctx.write(new DefaultHttp2DataFrame(buf));
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(trailers, true));
            } catch (Throwable e) {
                LOGGER.warn("Exception processing triple message", e);
                if (e instanceof TripleRpcException) {
                    responseErr(ctx, ((TripleRpcException) e).getStatus());
                } else {
                    responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                            .withDescription("Exception occurred in provider's execution:" + e.getMessage())
                            .withCause(e));
                }
            }
        };

        future.whenComplete(onComplete);
    }


    private Invocation buildInvocation() {

        RpcInvocation inv = new RpcInvocation();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        final List<MethodDescriptor> methods = serviceDescriptor.getMethods(methodName);
        if (methods == null || methods.isEmpty()) {
            responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED)
                    .withDescription("Method not found:" + methodName + " of service:" + serviceDescriptor.getServiceName()));
            return null;
        }
        if (methods.size() == 1) {
            this.methodDescriptor = methods.get(0);
            setNeedWrap(TripleUtil.needWrapper(this.methodDescriptor.getParameterClasses()));
        } else {
            // can not determine which one to invoke when same protobuf method name is used, force wrap it
            setNeedWrap(true);
        }
        if (isNeedWrap()) {
            loadFromURL(getUrl());
        }

        try {
            if (providerModel != null) {
                ClassLoadUtil.switchContextLoader(providerModel.getServiceInterfaceClass().getClassLoader());
            }
            if (isNeedWrap()) {
                final TripleWrapper.TripleRequestWrapper req = TripleUtil.unpack(getData(), TripleWrapper.TripleRequestWrapper.class);
                setSerializeType(req.getSerializeType());
                if (CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName)) {
                    this.methodDescriptor = repo.lookupMethod(GenericService.class.getName(), methodName);
                } else {
                    String[] paramTypes = req.getArgTypesList().toArray(new String[req.getArgsCount()]);
                    for (MethodDescriptor method : methods) {
                        if (Arrays.equals(method.getCompatibleParamSignatures(), paramTypes)) {
                            this.methodDescriptor = method;
                            break;
                        }
                    }
                    if (this.methodDescriptor == null) {
                        responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED)
                                .withDescription("Method not found:" + methodName +
                                        " args:" + Arrays.toString(paramTypes) + " of service:" + serviceDescriptor.getServiceName()));
                        return null;
                    }
                }
                final Object[] arguments = TripleUtil.unwrapReq(getUrl(), req, getMultipleSerialization());
                inv.setArguments(arguments);
            } else {

                final Object req = TripleUtil.unpack(getData(), methodDescriptor.getParameterClasses()[0]);
                inv.setArguments(new Object[]{req});
            }
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
        inv.setMethodName(methodDescriptor.getMethodName());
        inv.setServiceName(serviceDescriptor.getServiceName());
        inv.setTargetServiceUniqueName(getUrl().getServiceKey());
        inv.setParameterTypes(methodDescriptor.getParameterClasses());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        final Map<String, Object> attachments = parseHeadersToMap(getHeaders());
        attachments.put(TripleConstant.TRI_CHANNEL_CTX_KEY, ctx);
        inv.setObjectAttachments(attachments);
        return inv;
    }
}
