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
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.FutureContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;
import org.apache.dubbo.rpc.protocol.tri.call.ObserverToClientCallListenerAdapter;
import org.apache.dubbo.rpc.protocol.tri.call.TripleClientCall;
import org.apache.dubbo.rpc.protocol.tri.call.UnaryClientCallListener;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;
import org.apache.dubbo.rpc.support.RpcUtils;

import io.netty.util.AsciiString;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_DESTROY_INVOKER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_REQUEST;
import static org.apache.dubbo.rpc.Constants.COMPRESSOR_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.model.MethodDescriptor.RpcType.UNARY;

/**
 * TripleInvoker
 */
public class TripleInvoker<T> extends AbstractInvoker<T> {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(TripleInvoker.class);

    private final AbstractConnectionClient connectionClient;
    private final ReentrantLock destroyLock = new ReentrantLock();
    private final Set<Invoker<?>> invokers;
    private final ExecutorService streamExecutor;
    private final String acceptEncodings;
    private final TripleWriteQueue writeQueue = new TripleWriteQueue(256);

    public TripleInvoker(Class<T> serviceType,
        URL url,
        String acceptEncodings,
        AbstractConnectionClient connectionClient,
        Set<Invoker<?>> invokers,
        ExecutorService streamExecutor) {
        super(serviceType, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY});
        this.invokers = invokers;
        this.connectionClient = connectionClient;
        this.acceptEncodings = acceptEncodings;
        this.streamExecutor = streamExecutor;
    }

    private static AsciiString getSchemeFromUrl(URL url) {
        boolean ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY, false);
        return ssl ? TripleConstant.HTTPS_SCHEME : TripleConstant.HTTP_SCHEME;
    }

    private static Compressor getCompressorFromEnv() {
        Configuration configuration = ConfigurationUtils.getEnvConfiguration(ApplicationModel.defaultModel());
        String compressorKey = configuration.getString(COMPRESSOR_KEY, Identity.MESSAGE_ENCODING);
        return Compressor.getCompressor(ScopeModelUtil.getFrameworkModel(ApplicationModel.defaultModel()), compressorKey);
    }

    @Override
    protected Result doInvoke(final Invocation invocation) {
        if (!connectionClient.isConnected()) {
            CompletableFuture<AppResponse> future = new CompletableFuture<>();
            RpcException exception = TriRpcStatus.UNAVAILABLE.withDescription(
                    String.format("upstream %s is unavailable", getUrl().getAddress()))
                .asException();
            future.completeExceptionally(exception);
            return new AsyncRpcResult(future, invocation);
        }

        ConsumerModel consumerModel = (ConsumerModel) (invocation.getServiceModel() != null
            ? invocation.getServiceModel() : getUrl().getServiceModel());
        ServiceDescriptor serviceDescriptor = consumerModel.getServiceModel();
        final MethodDescriptor methodDescriptor = serviceDescriptor.getMethod(
            invocation.getMethodName(),
            invocation.getParameterTypes());
        Executor callbackExecutor = isSync(methodDescriptor, invocation) ? new ThreadlessExecutor() : streamExecutor;
        ClientCall call = new TripleClientCall(connectionClient, callbackExecutor,
            getUrl().getOrDefaultFrameworkModel(), writeQueue);
        AsyncRpcResult result;
        try {
            switch (methodDescriptor.getRpcType()) {
                case UNARY:
                    result = invokeUnary(methodDescriptor, invocation, call, callbackExecutor);
                    break;
                case SERVER_STREAM:
                    result = invokeServerStream(methodDescriptor, invocation, call);
                    break;
                case CLIENT_STREAM:
                case BI_STREAM:
                    result = invokeBiOrClientStream(methodDescriptor, invocation, call);
                    break;
                default:
                    throw new IllegalStateException("Can not reach here");
            }
            return result;
        } catch (Throwable t) {
            final TriRpcStatus status = TriRpcStatus.INTERNAL.withCause(t)
                .withDescription("Call aborted cause client exception");
            RpcException e = status.asException();
            try {
                call.cancelByLocal(e);
            } catch (Throwable t1) {
                LOGGER.error(PROTOCOL_FAILED_REQUEST, "", "", "Cancel triple request failed", t1);
            }
            CompletableFuture<AppResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return new AsyncRpcResult(future, invocation);
        }
    }

    private static boolean isSync(MethodDescriptor methodDescriptor, Invocation invocation){
        if (!(invocation instanceof RpcInvocation)) {
            return false;
        }
        RpcInvocation rpcInvocation = (RpcInvocation) invocation;
        MethodDescriptor.RpcType rpcType = methodDescriptor.getRpcType();
        return UNARY.equals(rpcType)
            && InvokeMode.SYNC.equals(rpcInvocation.getInvokeMode());
    }

    AsyncRpcResult invokeServerStream(MethodDescriptor methodDescriptor, Invocation invocation,
                                      ClientCall call) {
        RequestMetadata request = createRequest(methodDescriptor, invocation, null);
        StreamObserver<Object> responseObserver = (StreamObserver<Object>) invocation.getArguments()[1];
        final StreamObserver<Object> requestObserver = streamCall(call, request, responseObserver);
        requestObserver.onNext(invocation.getArguments()[0]);
        requestObserver.onCompleted();
        return new AsyncRpcResult(CompletableFuture.completedFuture(new AppResponse()), invocation);
    }

    AsyncRpcResult invokeBiOrClientStream(MethodDescriptor methodDescriptor, Invocation invocation,
                                          ClientCall call) {
        final AsyncRpcResult result;
        RequestMetadata request = createRequest(methodDescriptor, invocation, null);
        StreamObserver<Object> responseObserver = (StreamObserver<Object>) invocation.getArguments()[0];
        final StreamObserver<Object> requestObserver = streamCall(call, request, responseObserver);
        result = new AsyncRpcResult(
            CompletableFuture.completedFuture(new AppResponse(requestObserver)), invocation);
        return result;
    }

    StreamObserver<Object> streamCall(ClientCall call,
                                      RequestMetadata metadata,
                                      StreamObserver<Object> responseObserver) {
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(
            responseObserver);
        StreamObserver<Object> streamObserver = call.start(metadata, listener);
        if (responseObserver instanceof CancelableStreamObserver) {
            final CancellationContext context = new CancellationContext();
            CancelableStreamObserver<Object> cancelableStreamObserver = (CancelableStreamObserver<Object>) responseObserver;
            cancelableStreamObserver.setCancellationContext(context);
            context.addListener(context1 -> call.cancelByLocal(new IllegalStateException("Canceled by app")));
            listener.setOnStartConsumer(dummy -> cancelableStreamObserver.startRequest());
            cancelableStreamObserver.beforeStart((ClientCallToObserverAdapter<Object>) streamObserver);
        }
        return streamObserver;
    }

    AsyncRpcResult invokeUnary(MethodDescriptor methodDescriptor, Invocation invocation,
                               ClientCall call, Executor callbackExecutor) {

        int timeout = RpcUtils.calculateTimeout(getUrl(), invocation, invocation.getMethodName(), 3000);
        if (timeout <= 0) {
            return AsyncRpcResult.newDefaultAsyncResult(new RpcException(RpcException.TIMEOUT_TERMINATE,
                "No time left for making the following call: " + invocation.getServiceName() + "."
                    + invocation.getMethodName() + ", terminate directly."), invocation);
        }
        invocation.setAttachment(TIMEOUT_KEY, String.valueOf(timeout));

        final AsyncRpcResult result;
        DeadlineFuture future = DeadlineFuture.newFuture(getUrl().getPath(),
            methodDescriptor.getMethodName(), getUrl().getAddress(), timeout, callbackExecutor);

        RequestMetadata request = createRequest(methodDescriptor, invocation, timeout);

        final Object pureArgument;

        if (methodDescriptor instanceof StubMethodDescriptor) {
            pureArgument = invocation.getArguments()[0];
        } else {
            pureArgument = invocation.getArguments();
        }
        result = new AsyncRpcResult(future, invocation);
        FutureContext.getContext().setCompatibleFuture(future);

        result.setExecutor(callbackExecutor);
        ClientCall.Listener callListener = new UnaryClientCallListener(future);

        final StreamObserver<Object> requestObserver = call.start(request, callListener);
        requestObserver.onNext(pureArgument);
        requestObserver.onCompleted();
        return result;
    }

    RequestMetadata createRequest(MethodDescriptor methodDescriptor, Invocation invocation,
                                  Integer timeout) {
        final String methodName = RpcUtils.getMethodName(invocation);
        Objects.requireNonNull(methodDescriptor,
            "MethodDescriptor not found for" + methodName + " params:" + Arrays.toString(
                invocation.getCompatibleParamSignatures()));
        final RequestMetadata meta = new RequestMetadata();
        final URL url = getUrl();
        if (methodDescriptor instanceof PackableMethod) {
            meta.packableMethod = (PackableMethod) methodDescriptor;
        } else {
            meta.packableMethod = ReflectionPackableMethod.init(methodDescriptor, url);
        }
        meta.convertNoLowerHeader = TripleProtocol.CONVERT_NO_LOWER_HEADER;
        meta.ignoreDefaultVersion = TripleProtocol.IGNORE_1_0_0_VERSION;
        meta.method = methodDescriptor;
        meta.scheme = getSchemeFromUrl(url);
        meta.compressor = getCompressorFromEnv();
        meta.cancellationContext = RpcContext.getCancellationContext();
        meta.address = url.getAddress();
        meta.service = url.getPath();
        meta.group = url.getGroup();
        meta.version = url.getVersion();
        meta.acceptEncoding = acceptEncodings;
        if (timeout != null) {
            meta.timeout = timeout + "m";
        }
        String application = (String) invocation.getObjectAttachmentWithoutConvert(CommonConstants.APPLICATION_KEY);
        if (application == null) {
            application = (String) invocation.getObjectAttachmentWithoutConvert(CommonConstants.REMOTE_APPLICATION_KEY);
        }
        meta.application = application;
        meta.attachments = invocation.getObjectAttachments();
        return meta;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        return connectionClient.isConnected();
    }

    @Override
    public void destroy() {
        // in order to avoid closing a client multiple times, a counter is used in case of connection per jvm, every
        // time when client.close() is called, counter counts down once, and when counter reaches zero, client will be
        // closed.
        if (!super.isDestroyed()) {
            // double check to avoid dup close
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                if (invokers != null) {
                    invokers.remove(this);
                }
                try {
                    connectionClient.release();
                } catch (Throwable t) {
                    logger.warn(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", t.getMessage(), t);
                }

            } finally {
                destroyLock.unlock();
            }
        }
    }
}
