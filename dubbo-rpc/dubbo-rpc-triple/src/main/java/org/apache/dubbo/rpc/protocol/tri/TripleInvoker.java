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
import org.apache.dubbo.common.utils.CollectionUtils;
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
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.Unpack;
import org.apache.dubbo.rpc.protocol.tri.pack.VoidUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapReqPack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapRespUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamListener;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamClientListener;
import org.apache.dubbo.rpc.protocol.tri.stream.UnaryClientListener;
import org.apache.dubbo.rpc.support.RpcUtils;

import io.netty.util.AsciiString;

import java.util.Arrays;
import java.util.List;
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

    /**
     * Get the tri protocol special MethodDescriptor
     */
    private static MethodDescriptor lookupMethodDescriptor(ConsumerModel consumerModel, RpcInvocation inv) {
        List<MethodDescriptor> methodDescriptors = consumerModel.getServiceModel().getMethods(inv.getMethodName());
        if (CollectionUtils.isEmpty(methodDescriptors)) {
            throw new IllegalStateException("methodDescriptors must not be null method=" + inv.getMethodName());
        }
        for (MethodDescriptor methodDescriptor : methodDescriptors) {
            if (Arrays.equals(inv.getParameterTypes(), methodDescriptor.getRealParameterClasses())) {
                return methodDescriptor;
            }
        }
        throw new IllegalStateException("methodDescriptors must not be null method=" + inv.getMethodName());
    }

    private ClientStream createStream(RpcInvocation invocation, long id, String methodName, String timeout, ExecutorService executor) {
        String application = (String) invocation.getObjectAttachments().remove(CommonConstants.APPLICATION_KEY);
        if (application == null) {
            application = (String) invocation.getObjectAttachments().remove(CommonConstants.REMOTE_APPLICATION_KEY);
        }
        ConsumerModel consumerModel = invocation.getServiceModel() != null ? (ConsumerModel) invocation.getServiceModel() : (ConsumerModel) getUrl().getServiceModel();
        MethodDescriptor methodDescriptor = lookupMethodDescriptor(consumerModel, invocation);
        org.apache.dubbo.rpc.protocol.tri.stream.ClientStream stream = null;

        Pack requestPack;
        Unpack responseUnpack;
        if (methodDescriptor.isNeedWrap()) {
            requestPack = new WrapReqPack(invocation.getParameterTypes(), genericPack, PbPack.INSTANCE);
            if (!Void.TYPE.equals(methodDescriptor.getReturnClass())) {
                responseUnpack = new WrapRespUnpack(genericUnpack);
            } else {
                responseUnpack = VoidUnpack.INSTANCE;
            }
        } else {
            requestPack = PbPack.INSTANCE;
            responseUnpack = new PbUnpack(methodDescriptor.getReturnClass());
        }
        ClientStreamListener listener;
        if (methodDescriptor.isUnary()) {
            listener = new UnaryClientListener(connection, id);
        } else {
            listener = new StreamClientListener(null, null);
        }
        stream = new org.apache.dubbo.rpc.protocol.tri.stream.ClientStream(
            getUrl(),
            executor,
            id,
            connection.getChannel(),
            scheme,
            "/" + getUrl().getPath() + "/" + methodName,
            getUrl().getVersion(),
            getUrl().getGroup(),
            application,
            getUrl().getAddress(),
            compressor.getMessageEncoding(),
            acceptEncoding,
            timeout,
            compressor,
            invocation.getObjectAttachments(),
            requestPack,
            responseUnpack,
            listener);
        return stream;
    }

    @Override
    protected Result doInvoke(final Invocation invocation) throws Throwable {
//        try {
        Request req = new Request();
        req.setVersion(TripleConstant.TRI_VERSION);
        req.setTwoWay(true);
        req.setData(invocation);
        final String methodName = RpcUtils.getMethodName(invocation);
        int timeout = calculateTimeout(invocation, methodName);

        ExecutorService executor = getCallbackExecutor(getUrl(), invocation);
        DefaultFuture2 future = DefaultFuture2.newFuture(this.connection, req, timeout, executor);
        final CompletableFuture<AppResponse> respFuture = future.thenApply(obj -> (AppResponse) obj);
        final org.apache.dubbo.rpc.protocol.tri.stream.ClientStream stream = createStream((RpcInvocation) invocation, req.getId(), methodName, timeout + "m", executor);
//        inv.setServiceModel(RpcContext.getServiceContext().getConsumerUrl().getServiceModel());
//        inv.setAttachment(PATH_KEY, getUrl().getPath());
//        inv.setAttachment(Constants.SERIALIZATION_KEY,
//            getUrl().getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
        // create request.


        // try connect
        connection.isAvailable();


        // save for 2.6.x compatibility, for example, TraceFilter in Zipkin uses com.alibaba.xxx.FutureAdapter
        FutureContext.getContext().setCompatibleFuture(respFuture);
        AsyncRpcResult result = new AsyncRpcResult(respFuture, invocation);
        result.setExecutor(executor);

        if (!connection.isAvailable()) {
            Response response = new Response(req.getId(), req.getVersion());
            response.setStatus(Response.CHANNEL_INACTIVE);
            response.setErrorMessage(String.format("Connect to %s failed", this));
            DefaultFuture2.received(connection, response);
        } else {
            stream.startCall();
            stream.sendMessage(invocation.getArguments());
            DefaultFuture2.sent(req);
//                        Response response = new Response(req.getId(), req.getVersion());
//                        response.setStatus(Response.CHANNEL_INACTIVE);
//                        response.setErrorMessage(StringUtils.toString(future1.cause()));
//                        DefaultFuture2.received(connection, response);
        }

        return result;
//        } catch (TimeoutException e) {
//            throw new RpcException(RpcException.TIMEOUT_EXCEPTION,
//                "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl()
//                    + ", cause: " + e.getMessage(), e);
//        } catch (RemotingException e) {
//            throw new RpcException(RpcException.NETWORK_EXCEPTION,
//                "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl()
//                    + ", cause: " + e.getMessage(), e);
//        }
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
