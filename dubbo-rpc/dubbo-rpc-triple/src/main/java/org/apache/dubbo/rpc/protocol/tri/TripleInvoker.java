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
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.api.ConnectionManager;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
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
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.support.RpcUtils;

import io.netty.util.AsciiString;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_ATTACHMENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;
import static org.apache.dubbo.rpc.Constants.COMPRESSOR_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * TripleInvoker
 */
public class TripleInvoker<T> extends AbstractInvoker<T> {

    private final Connection connection;
    private final ReentrantLock destroyLock = new ReentrantLock();
    private final AsciiString scheme;
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
        this.scheme = getSchemeFromUrl(url);
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
    protected Result doInvoke(final Invocation invocation) throws Throwable {
        connection.isAvailable();
        ExecutorService executor = getCallbackExecutor(getUrl(), invocation);
        final String methodName = RpcUtils.getMethodName(invocation);
        int timeout = calculateTimeout(invocation, methodName);
        Request req = new Request();
        req.setVersion(TripleConstant.TRI_VERSION);
        req.setTwoWay(true);
        req.setData(invocation);
        DefaultFuture2 future = DefaultFuture2.newFuture(this.connection, req, timeout, executor);
        final CompletableFuture<AppResponse> respFuture = future.thenApply(obj -> (AppResponse) obj);
        FutureContext.getContext().setCompatibleFuture(respFuture);
        AsyncRpcResult result = new AsyncRpcResult(respFuture, invocation);
        result.setExecutor(executor);

        if (!connection.isAvailable()) {
            Response response = new Response(req.getId(), req.getVersion());
            response.setStatus(Response.CHANNEL_INACTIVE);
            response.setErrorMessage(String.format("Connect to %s failed", this));
            DefaultFuture2.received(connection, response);
            return result;
        }
        ConsumerModel consumerModel = invocation.getServiceModel() != null ? (ConsumerModel) invocation.getServiceModel() : (ConsumerModel) getUrl().getServiceModel();
        MethodDescriptor methodDescriptor = consumerModel.getServiceModel().getMethod(methodName, invocation.getParameterTypes());
        if(methodDescriptor==null){
            throw new IllegalStateException("MethodDescriptor not found for"+methodName+" params:"+ Arrays.toString(invocation.getCompatibleParamSignatures()));
        }
        String application = (String) invocation.getObjectAttachments().get(CommonConstants.APPLICATION_KEY);
        if (application == null) {
            application = (String) invocation.getObjectAttachments().get(CommonConstants.REMOTE_APPLICATION_KEY);
        }
        ClientCall call = new ClientCall(getUrl(),
            req.getId(),
            connection,
            scheme,
            getUrl().getPath(),
            getUrl().getVersion(),
            getUrl().getGroup(),
            application,
            getUrl().getAddress(),
            timeout + "m",
            methodName,
            acceptEncoding,
            compressor,
            invocation.getObjectAttachments(),
            genericUnpack,
            executor,
            methodDescriptor);
        if (methodDescriptor instanceof StreamMethodDescriptor) {
            final StreamObserver<Object> requestObserver = ClientCall.streamCall(call, ClientCall.getObserver(methodDescriptor, invocation.getArguments()));
            DefaultFuture2.sent(req);
            AppResponse appResponse = new AppResponse();
            appResponse.setValue(requestObserver);
            Response response = new Response(req.getId(), TripleConstant.TRI_VERSION);
            response.setResult(appResponse);
            DefaultFuture2.received(connection, response);
        } else {
            ClientCall.unaryCall(call, WrapUtils.getRequest(genericPack, methodDescriptor, invocation.getArguments()));
        }
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
        Object countdown = RpcContext.getClientAttachment().getObjectAttachment(TIME_COUNTDOWN_KEY);
        int timeout;
        if (countdown == null) {
            timeout = (int) RpcUtils.getTimeout(getUrl(), methodName, RpcContext.getClientAttachment(), DEFAULT_TIMEOUT);
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

    private AsciiString getSchemeFromUrl(URL url) {
        try {
            Boolean ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY, Boolean.class);
            if (ssl == null) {
                return TripleConstant.HTTP_SCHEME;
            }
            return ssl ? TripleConstant.HTTPS_SCHEME : TripleConstant.HTTP_SCHEME;
        } catch (Exception e) {
            return TripleConstant.HTTP_SCHEME;
        }
    }

}
