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
package org.apache.dubbo.rpc.protocol.thrift;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.AtomicPositiveInteger;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;


/**
 * @since 2.7.0, use https://github.com/dubbo/dubbo-rpc-native-thrift instead
 */
@Deprecated
public class ThriftInvoker<T> extends AbstractInvoker<T> {

    private final ExchangeClient[] clients;

    private final AtomicPositiveInteger index = new AtomicPositiveInteger();

    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    public ThriftInvoker(Class<T> service, URL url, ExchangeClient[] clients) {
        this(service, url, clients, null);
    }

    public ThriftInvoker(Class<T> type, URL url, ExchangeClient[] clients, Set<Invoker<?>> invokers) {
        super(type, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY, TIMEOUT_KEY});
        this.clients = clients;
        this.invokers = invokers;
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {

        RpcInvocation inv = (RpcInvocation) invocation;

        final String methodName;

        methodName = invocation.getMethodName();

        inv.setAttachment(PATH_KEY, getUrl().getPath());

        // for thrift codec
        inv.setAttachment(ThriftCodec.PARAMETER_CLASS_NAME_GENERATOR, getUrl().getParameter(
                ThriftCodec.PARAMETER_CLASS_NAME_GENERATOR, DubboClassNameGenerator.NAME));

        ExchangeClient currentClient;

        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }

        try {
            int timeout = getUrl().getMethodParameter(methodName, TIMEOUT_KEY, DEFAULT_TIMEOUT);

            RpcContext.getContext().setFuture(null);

            return (Result) currentClient.request(inv, timeout).get();

        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, e.getMessage(), e);
        }

    }

    @Override
    public boolean isAvailable() {

        if (!super.isAvailable()) {
            return false;
        }

        for (ExchangeClient client : clients) {
            if (client.isConnected()
                    && !client.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY)) {
                //cannot write == not Available ?
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // in order to avoid closing a client multiple times, a counter is used in case of connection per jvm, every
        // time when client.close() is called, counter counts down once, and when counter reaches zero, client will be
        // closed.
        if (super.isDestroyed()) {
            return;
        } else {
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

                for (ExchangeClient client : clients) {

                    try {
                        client.close();
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }

                }

            } finally {
                destroyLock.unlock();
            }

        }

    }

}
