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
package org.apache.dubbo.rpc.listener;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.InvokerListener;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;
import java.util.function.Consumer;

/**
 * ListenerInvoker
 */
public class ListenerInvokerWrapper<T> implements Invoker<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ListenerInvokerWrapper.class);

    private final Invoker<T> invoker;

    private final List<InvokerListener> listeners;

    public ListenerInvokerWrapper(Invoker<T> invoker, List<InvokerListener> listeners) {
        if (invoker == null) {
            throw new IllegalArgumentException("invoker == null");
        }
        this.invoker = invoker;
        this.listeners = listeners;
        listenerEvent(listener -> listener.referred(invoker));
    }


    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public String toString() {
        return getInterface() + " -> " + (getUrl() == null ? " " : getUrl().toString());
    }

    @Override
    public void destroy() {
        try {
            invoker.destroy();
        } finally {
            listenerEvent(listener -> listener.destroyed(invoker));
        }
    }

    public Invoker<T> getInvoker() {
        return invoker;
    }

    public List<InvokerListener> getListeners() {
        return listeners;
    }

    private void listenerEvent(Consumer<InvokerListener> consumer) {
        if (CollectionUtils.isNotEmpty(listeners)) {
            RuntimeException exception = null;
            for (InvokerListener listener : listeners) {
                if (listener != null) {
                    try {
                        consumer.accept(listener);
                    } catch (RuntimeException t) {
                        logger.error(LoggerCodeConstants.INTERNAL_ERROR, "wrapped listener internal error", "", t.getMessage(), t);
                        exception = t;
                    }
                }
            }

            if (exception != null) {
                throw exception;
            }
        }
    }
}
