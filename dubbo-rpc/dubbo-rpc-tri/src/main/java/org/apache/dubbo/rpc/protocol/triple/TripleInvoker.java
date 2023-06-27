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

package org.apache.dubbo.rpc.protocol.triple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_DESTROY_INVOKER;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

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

    @Override
    protected Result doInvoke(final Invocation invocation) {
        if (!connectionClient.isConnected()) {
            CompletableFuture<AppResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException());
            return new AsyncRpcResult(future, invocation);
        }

        ConsumerModel consumerModel = (ConsumerModel) (invocation.getServiceModel() != null
            ? invocation.getServiceModel() : getUrl().getServiceModel());
        ServiceDescriptor serviceDescriptor = consumerModel.getServiceModel();
        final MethodDescriptor methodDescriptor = serviceDescriptor.getMethod(
            invocation.getMethodName(),
            invocation.getParameterTypes());
        try {
            return null;

        } catch (Throwable t) {
            CompletableFuture<AppResponse> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return new AsyncRpcResult(future, invocation);
        }
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
