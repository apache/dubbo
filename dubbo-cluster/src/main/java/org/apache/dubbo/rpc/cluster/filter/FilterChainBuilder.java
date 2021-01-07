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
package org.apache.dubbo.rpc.cluster.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;

@SPI("default")
public interface FilterChainBuilder {
    <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group);

    <T> ClusterInvoker<T> buildClusterInvokerChain(final ClusterInvoker<T> invoker, String key, String group);

    class FilterChainNode<T, TYPE extends Invoker<T>> implements Invoker<T>{
        TYPE originalInvoker;
        Invoker<T> nextNode;
        Filter filter;

        public FilterChainNode(TYPE originalInvoker, Invoker<T> nextNode, Filter filter) {
            this.originalInvoker = originalInvoker;
            this.nextNode = nextNode;
            this.filter = filter;
        }

        public TYPE getOriginalInvoker() {
            return originalInvoker;
        }

        @Override
        public Class<T> getInterface() {
            return originalInvoker.getInterface();
        }

        @Override
        public URL getUrl() {
            return originalInvoker.getUrl();
        }

        @Override
        public boolean isAvailable() {
            return originalInvoker.isAvailable();
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            Result asyncResult;
            try {
                asyncResult = filter.invoke(nextNode, invocation);
            } catch (Exception e) {
                if (filter instanceof ListenableFilter) {
                    ListenableFilter listenableFilter = ((ListenableFilter) filter);
                    try {
                        Filter.Listener listener = listenableFilter.listener(invocation);
                        if (listener != null) {
                            listener.onError(e, originalInvoker, invocation);
                        }
                    } finally {
                        listenableFilter.removeListener(invocation);
                    }
                } else if (filter instanceof Filter.Listener) {
                    Filter.Listener listener = (Filter.Listener) filter;
                    listener.onError(e, originalInvoker, invocation);
                }
                throw e;
            } finally {

            }
            return asyncResult.whenCompleteWithContext((r, t) -> {
                if (filter instanceof ListenableFilter) {
                    ListenableFilter listenableFilter = ((ListenableFilter) filter);
                    Filter.Listener listener = listenableFilter.listener(invocation);
                    try {
                        if (listener != null) {
                            if (t == null) {
                                listener.onResponse(r, originalInvoker, invocation);
                            } else {
                                listener.onError(t, originalInvoker, invocation);
                            }
                        }
                    } finally {
                        listenableFilter.removeListener(invocation);
                    }
                } else if (filter instanceof Filter.Listener) {
                    Filter.Listener listener = (Filter.Listener) filter;
                    if (t == null) {
                        listener.onResponse(r, originalInvoker, invocation);
                    } else {
                        listener.onError(t, originalInvoker, invocation);
                    }
                }
            });
        }

        @Override
        public void destroy() {
            originalInvoker.destroy();
        }

        @Override
        public String toString() {
            return originalInvoker.toString();
        }
    }

    class ClusterFilterChainNode<T, TYPE extends ClusterInvoker<T>> extends FilterChainNode<T, TYPE> implements ClusterInvoker<T> {
        public ClusterFilterChainNode(TYPE originalInvoker, Invoker<T> nextNode, Filter filter) {
            super(originalInvoker, nextNode, filter);
        }


        @Override
        public URL getRegistryUrl() {
            return getOriginalInvoker().getRegistryUrl();
        }

        @Override
        public Directory<T> getDirectory() {
            return getOriginalInvoker().getDirectory();
        }

        @Override
        public boolean isDestroyed() {
            return getOriginalInvoker().isDestroyed();
        }
    }
}
