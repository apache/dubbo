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
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
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
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import io.netty.channel.ChannelFuture;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_ATTACHMENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * TripleInvoker
 */
public class TripleInvoker<T> extends AbstractInvoker<T> {

    private final Connection connection;
    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    public TripleInvoker(Class<T> serviceType, URL url, Set<Invoker<?>> invokers) throws RemotingException {
        super(serviceType, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY});
        this.invokers = invokers;
        ConnectionManager connectionManager = url.getOrDefaultFrameworkModel().getExtensionLoader(ConnectionManager.class).getExtension("multiple");
        this.connection = connectionManager.connect(url);
    }

    @Override
    protected Result doInvoke(final Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;

        final String methodName = RpcUtils.getMethodName(invocation);
        inv.setServiceModel(RpcContext.getServiceContext().getConsumerUrl().getServiceModel());
        inv.setAttachment(PATH_KEY, getUrl().getPath());
        inv.setAttachment(Constants.SERIALIZATION_KEY,
            getUrl().getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
        try {
            int timeout = calculateTimeout(invocation, methodName);
            invocation.setAttachment(TIMEOUT_KEY, timeout);
            ExecutorService executor = getCallbackExecutor(getUrl(), inv);
            // create request.
            Request req = new Request();
            req.setVersion(Version.getProtocolVersion());
            req.setTwoWay(true);
            req.setData(inv);

            // try connect
            connection.isAvailable();

            DefaultFuture2 future = DefaultFuture2.newFuture(this.connection, req, timeout, executor);
            final CompletableFuture<AppResponse> respFuture = future.thenApply(obj -> (AppResponse) obj);
            // save for 2.6.x compatibility, for example, TraceFilter in Zipkin uses com.alibaba.xxx.FutureAdapter
            FutureContext.getContext().setCompatibleFuture(respFuture);
            AsyncRpcResult result = new AsyncRpcResult(respFuture, inv);
            result.setExecutor(executor);

            if (!connection.isAvailable()) {
                Response response = new Response(req.getId(), req.getVersion());
                response.setStatus(Response.CHANNEL_INACTIVE);
                response.setErrorMessage(String.format("Connect to %s failed", this));
                DefaultFuture2.received(connection, response);
            } else {
                final ChannelFuture writeFuture = this.connection.write(req);
                writeFuture.addListener(future1 -> {
                    if (future1.isSuccess()) {
                        DefaultFuture2.sent(req);
                    } else {
                        Response response = new Response(req.getId(), req.getVersion());
                        response.setStatus(Response.CHANNEL_INACTIVE);
                        response.setErrorMessage(StringUtils.toString(future1.cause()));
                        DefaultFuture2.received(connection, response);
                    }
                });
            }

            return result;
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION,
                "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl()
                    + ", cause: " + e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION,
                "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl()
                    + ", cause: " + e.getMessage(), e);
        }
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
}
