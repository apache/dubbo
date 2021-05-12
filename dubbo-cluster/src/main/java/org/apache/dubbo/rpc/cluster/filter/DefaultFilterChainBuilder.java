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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import java.util.List;

@Activate(order = 0)
public class DefaultFilterChainBuilder implements FilterChainBuilder {

    /**
     * build consumer/provider filter chain
     */
    @Override
    public <T> Invoker<T> buildInvokerChain(final Invoker<T> originalInvoker, String key, String group) {
        Invoker<T> last = originalInvoker;
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(originalInvoker.getUrl(), key, group);

        if (!filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                final Invoker<T> next = last;
                if (filter instanceof BaseFilter.Request) {
                    if (last instanceof LoopFilterChainNode) {
                        ((LoopFilterChainNode<T, Invoker<T>, BaseFilter.Request>) last).addFirstFilter((BaseFilter.Request) filter);
                    } else {
                        last = new LoopFilterChainNode<>(originalInvoker, next, (BaseFilter.Request) filter);
                    }
                } else {
                    last = new FilterChainNode<>(originalInvoker, next, filter);
                }
            }
        }

        return last;
    }

    /**
     * build consumer cluster filter chain
     */
    @Override
    public <T> ClusterInvoker<T> buildClusterInvokerChain(final ClusterInvoker<T> originalInvoker, String key, String group) {
        ClusterInvoker<T> last = originalInvoker;
        List<ClusterFilter> filters = ExtensionLoader.getExtensionLoader(ClusterFilter.class).getActivateExtension(originalInvoker.getUrl(), key, group);

        if (!filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                final ClusterFilter filter = filters.get(i);
                final Invoker<T> next = last;
                if (filter instanceof BaseFilter.Request) {
                    if (last instanceof ClusterLoopFilterChainNode) {
                        ((ClusterLoopFilterChainNode<T, ClusterInvoker<T>, BaseFilter.Request>) last).addFirstFilter((BaseFilter.Request) filter);
                    } else {
                        last = new ClusterLoopFilterChainNode<>(originalInvoker, next, (BaseFilter.Request) filter);
                    }
                } else {
                    last = new ClusterFilterChainNode<>(originalInvoker, next, filter);
                }
            }
        }

        return last;
    }

}
