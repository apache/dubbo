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
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReferenceCountInvokerWrapper<T> implements Invoker<T> {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ReferenceCountInvokerWrapper.class);
    private final Invoker<T> invoker;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public ReferenceCountInvokerWrapper(Invoker<T> invoker) {
        this.invoker = invoker;
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return !destroyed.get() && invoker.isAvailable();
    }

    @Override
    public void destroy() {
        try {
            lock.writeLock().lock();
            destroyed.set(true);
        } finally {
            lock.writeLock().unlock();
        }
        invoker.destroy();
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        try {
            lock.readLock().lock();
            if (destroyed.get()) {
                logger.warn(LoggerCodeConstants.PROTOCOL_CLOSED_SERVER, "", "",
                    "Remote invoker has been destroyed, and unable to invoke anymore.");
                throw new RpcException("This invoker has been destroyed!");
            }
            return invoker.invoke(invocation);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Invoker<T> getInvoker() {
        return invoker;
    }
}
