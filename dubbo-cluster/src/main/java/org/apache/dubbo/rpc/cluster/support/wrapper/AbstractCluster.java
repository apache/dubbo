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
package org.apache.dubbo.rpc.cluster.support.wrapper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.filter.FilterChainBuilder;
import org.apache.dubbo.rpc.cluster.filter.InvocationInterceptorBuilder;
import org.apache.dubbo.rpc.cluster.interceptor.ClusterInterceptor;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_INTERCEPTOR_COMPATIBLE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INVOCATION_INTERCEPTOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;

public abstract class AbstractCluster implements Cluster {

    private <T> Invoker<T> buildClusterInterceptors(AbstractClusterInvoker<T> clusterInvoker) {
        AbstractClusterInvoker<T> last = buildInterceptorInvoker(new ClusterFilterInvoker<>(clusterInvoker));

        if (Boolean.parseBoolean(ConfigurationUtils.getProperty(clusterInvoker.getDirectory().getConsumerUrl().getScopeModel(), CLUSTER_INTERCEPTOR_COMPATIBLE_KEY, "false"))) {
            return build27xCompatibleClusterInterceptors(clusterInvoker, last);
        }
        return last;
    }

    @Override
    public <T> Invoker<T> join(Directory<T> directory, boolean buildFilterChain) throws RpcException {
        if (buildFilterChain) {
            return buildClusterInterceptors(doJoin(directory));
        } else {
            return doJoin(directory);
        }
    }

    private <T> AbstractClusterInvoker<T> buildInterceptorInvoker(AbstractClusterInvoker<T> invoker) {
        List<InvocationInterceptorBuilder> builders = ScopeModelUtil.getApplicationModel(invoker.getUrl().getScopeModel()).getExtensionLoader(InvocationInterceptorBuilder.class).getActivateExtensions();
        if (CollectionUtils.isEmpty(builders)) {
            return invoker;
        }
        return new InvocationInterceptorInvoker<>(invoker, builders);
    }

    protected abstract <T> AbstractClusterInvoker<T> doJoin(Directory<T> directory) throws RpcException;

    static class ClusterFilterInvoker<T> extends AbstractClusterInvoker<T> {
        private final ClusterInvoker<T> filterInvoker;

        public ClusterFilterInvoker(AbstractClusterInvoker<T> invoker) {
            List<FilterChainBuilder> builders = ScopeModelUtil.getApplicationModel(invoker.getUrl().getScopeModel()).getExtensionLoader(FilterChainBuilder.class).getActivateExtensions();
            if (CollectionUtils.isEmpty(builders)) {
                filterInvoker = invoker;
            } else {
                ClusterInvoker<T> tmpInvoker = invoker;
                for (FilterChainBuilder builder : builders) {
                    tmpInvoker = builder.buildClusterInvokerChain(tmpInvoker, REFERENCE_FILTER_KEY, CommonConstants.CONSUMER);
                }
                filterInvoker = tmpInvoker;
            }
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return filterInvoker.invoke(invocation);
        }

        @Override
        public Directory<T> getDirectory() {
            return filterInvoker.getDirectory();
        }



        @Override
        public URL getRegistryUrl() {
            return filterInvoker.getRegistryUrl();
        }

        @Override
        public boolean isDestroyed() {
            return filterInvoker.isDestroyed();
        }

        @Override
        public URL getUrl() {
            return filterInvoker.getUrl();
        }

        /**
         * The only purpose is to build a interceptor chain, so the cluster related logic doesn't matter.
         * Use ClusterInvoker<T> to replace AbstractClusterInvoker<T> in the future.
         */
        @Override
        protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
           return null;
        }

        public ClusterInvoker<T> getFilterInvoker() {
            return filterInvoker;
        }
    }

    static class InvocationInterceptorInvoker<T> extends AbstractClusterInvoker<T> {
        private ClusterInvoker<T> interceptorInvoker;

        public InvocationInterceptorInvoker(AbstractClusterInvoker<T> invoker, List<InvocationInterceptorBuilder> builders) {
            ClusterInvoker<T> tmpInvoker = invoker;
            for (InvocationInterceptorBuilder builder : builders) {
                tmpInvoker = builder.buildClusterInterceptorChain(tmpInvoker, INVOCATION_INTERCEPTOR_KEY, CommonConstants.CONSUMER);
            }
            interceptorInvoker = tmpInvoker;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return interceptorInvoker.invoke(invocation);
        }

        @Override
        public Directory<T> getDirectory() {
            return interceptorInvoker.getDirectory();
        }

        @Override
        public URL getRegistryUrl() {
            return interceptorInvoker.getRegistryUrl();
        }

        @Override
        public boolean isDestroyed() {
            return interceptorInvoker.isDestroyed();
        }

        @Override
        public URL getUrl() {
            return interceptorInvoker.getUrl();
        }

        /**
         * The only purpose is to build a interceptor chain, so the cluster related logic doesn't matter.
         * Use ClusterInvoker<T> to replace AbstractClusterInvoker<T> in the future.
         */
        @Override
        protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
            return null;
        }
    }

    @Deprecated
    private <T> ClusterInvoker<T> build27xCompatibleClusterInterceptors(AbstractClusterInvoker<T> clusterInvoker, AbstractClusterInvoker<T> last) {
        List<ClusterInterceptor> interceptors = ScopeModelUtil.getApplicationModel(clusterInvoker.getUrl().getScopeModel()).getExtensionLoader(ClusterInterceptor.class).getActivateExtensions();

        if (!interceptors.isEmpty()) {
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                final ClusterInterceptor interceptor = interceptors.get(i);
                final AbstractClusterInvoker<T> next = last;
                last = new InterceptorInvokerNode<>(clusterInvoker, interceptor, next);
            }
        }
        return last;
    }

    @Deprecated
    static class InterceptorInvokerNode<T> extends AbstractClusterInvoker<T> {

        private final AbstractClusterInvoker<T> clusterInvoker;
        private final ClusterInterceptor interceptor;
        private final AbstractClusterInvoker<T> next;

        public InterceptorInvokerNode(AbstractClusterInvoker<T> clusterInvoker,
                                      ClusterInterceptor interceptor,
                                      AbstractClusterInvoker<T> next) {
            this.clusterInvoker = clusterInvoker;
            this.interceptor = interceptor;
            this.next = next;
        }

        @Override
        public Class<T> getInterface() {
            return clusterInvoker.getInterface();
        }

        @Override
        public URL getUrl() {
            return clusterInvoker.getUrl();
        }

        @Override
        public boolean isAvailable() {
            return clusterInvoker.isAvailable();
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            Result asyncResult;
            try {
                interceptor.before(next, invocation);
                asyncResult = interceptor.intercept(next, invocation);
            } catch (Exception e) {
                // onError callback
                if (interceptor instanceof ClusterInterceptor.Listener) {
                    ClusterInterceptor.Listener listener = (ClusterInterceptor.Listener) interceptor;
                    listener.onError(e, clusterInvoker, invocation);
                }
                throw e;
            } finally {
                interceptor.after(next, invocation);
            }
            return asyncResult.whenCompleteWithContext((r, t) -> {
                // onResponse callback
                if (interceptor instanceof ClusterInterceptor.Listener) {
                    ClusterInterceptor.Listener listener = (ClusterInterceptor.Listener) interceptor;
                    if (t == null) {
                        listener.onMessage(r, clusterInvoker, invocation);
                    } else {
                        listener.onError(t, clusterInvoker, invocation);
                    }
                }
            });
        }

        @Override
        public void destroy() {
            clusterInvoker.destroy();
        }

        @Override
        public String toString() {
            return clusterInvoker.toString();
        }

        @Override
        protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
            // The only purpose is to build an interceptor chain, so the cluster related logic doesn't matter.
            return null;
        }
    }


}
