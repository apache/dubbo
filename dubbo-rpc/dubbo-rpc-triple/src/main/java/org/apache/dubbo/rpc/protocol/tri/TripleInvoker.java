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
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.api.ConnectionManager;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.FutureContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCallUtil;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_ATTACHMENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;
import static org.apache.dubbo.rpc.Constants.COMPRESSOR_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * TripleInvoker
 */
public class TripleInvoker<T> extends AbstractInvoker<T> {

    private final Connection connection;
    private final ReentrantLock destroyLock = new ReentrantLock();
    private final Compressor compressor;
    private final String acceptEncoding;
    private final Set<Invoker<?>> invokers;
    private final GenericPack genericPack;
    private final GenericUnpack genericUnpack;

    public TripleInvoker(Class<T> serviceType,
                         URL url,
                         MultipleSerialization serialization,
                         String serializationName,
                         Compressor defaultCompressor,
                         String acceptEncoding,
                         ConnectionManager connectionManager,
                         Set<Invoker<?>> invokers) throws RemotingException {
        super(serviceType, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY});
        this.genericPack = new GenericPack(serialization, serializationName, url);
        this.genericUnpack = new GenericUnpack(serialization, url);
        this.invokers = invokers;
        this.acceptEncoding = acceptEncoding;
        this.connection = connectionManager.connect(url);
        String compressorStr = url.getParameter(COMPRESSOR_KEY);
        if (compressorStr == null) {
            compressor = defaultCompressor;
        } else {
            compressor = Compressor.getCompressor(url.getOrDefaultFrameworkModel(), compressorStr);
        }
    }


    @Override
    protected Result doInvoke(final Invocation invocation) {
        URL url = getUrl();
        ExecutorService callbackExecutor = getCallbackExecutor(url, invocation);
        int timeout = calculateTimeout(invocation, invocation.getMethodName());
        invocation.setAttachment(TIMEOUT_KEY, timeout);
        DefaultFuture2 future = DefaultFuture2.newFuture(this.connection, invocation, timeout, callbackExecutor);
        final CompletableFuture<AppResponse> respFuture = future.thenApply(obj -> (AppResponse) obj);
        FutureContext.getContext().setCompatibleFuture(respFuture);
        AsyncRpcResult result = new AsyncRpcResult(respFuture, invocation);
        result.setExecutor(callbackExecutor);

        if (!connection.isAvailable()) {
            final RpcStatus status = RpcStatus.UNAVAILABLE
                    .withDescription(String.format("Connect to %s failed", this));
            DefaultFuture2.received(future.requestId, status, null);
            return result;
        }
        ConsumerModel consumerModel = invocation.getServiceModel() != null ?
                (ConsumerModel) invocation.getServiceModel() : (ConsumerModel) getUrl().getServiceModel();
        final MethodDescriptor methodDescriptor = consumerModel.getServiceModel()
                .getMethod(invocation.getMethodName(), invocation.getParameterTypes());
        final RequestMetadata metadata = StreamUtils.createRequest(getUrl(), methodDescriptor, invocation, future.requestId,
                compressor, acceptEncoding, timeout, genericPack, genericUnpack);
        ExecutorService executor = url.getOrDefaultApplicationModel().getExtensionLoader(ExecutorRepository.class)
                .getDefaultExtension()
                .getExecutor(url);
        if (executor == null) {
            throw new IllegalStateException("No available executor found in " + url);
        }
        ClientCall call = new ClientCall(connection, executor, url.getOrDefaultFrameworkModel());
        ClientCallUtil.call(call, metadata);
        return result;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        return connection.isAvailable();
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
                    connection.release();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }

            } finally {
                destroyLock.unlock();
            }
        }
    }

    private int calculateTimeout(Invocation invocation, String methodName) {
        if (invocation.get(TIMEOUT_KEY) != null) {
            return (int) invocation.get(TIMEOUT_KEY);
        }
        Object countdown = RpcContext.getClientAttachment().getObjectAttachment(TIME_COUNTDOWN_KEY);
        int timeout;
        if (countdown == null) {
            timeout = (int) RpcUtils.getTimeout(getUrl(), methodName, RpcContext.getClientAttachment(), 3000);
            if (getUrl().getParameter(ENABLE_TIMEOUT_COUNTDOWN_KEY, false)) {
                invocation.setObjectAttachment(TIMEOUT_ATTACHMENT_KEY, timeout); // pass timeout to remote server
            }
        } else {
            TimeoutCountDown timeoutCountDown = (TimeoutCountDown) countdown;
            timeout = (int) timeoutCountDown.timeRemaining(TimeUnit.MILLISECONDS);
            invocation.setObjectAttachment(TIMEOUT_ATTACHMENT_KEY, timeout);// pass timeout to remote server
        }
        return timeout;
    }

}
