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

package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.wrapper.AbstractCluster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLUSTER_KEY;

public class DynamicClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private final Function<String, AbstractCluster> builder;


    private final Map<String, AbstractClusterInvoker<T>> clusterInvokerMap = new ConcurrentHashMap<>();

    public DynamicClusterInvoker(Directory<T> directory, Function<String, AbstractCluster> clusterBuilder) {
        super(directory);
        this.builder = clusterBuilder;
    }


    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        String cluster;
        cluster = invocation.getAttachment(CLUSTER_KEY);

        if (StringUtils.isEmpty(cluster)) {
            cluster = RpcContext.getServiceContext().getConsumerUrl().getMethodParameter(invocation.getMethodName(), CLUSTER_KEY);
        }
        if (StringUtils.isEmpty(cluster)) {
            cluster = RpcContext.getServiceContext().getConsumerUrl().getParameter(DEFAULT_CLUSTER_KEY);
        }

        String finalCluster = cluster;
        AbstractClusterInvoker<T> abstractClusterInvoker = clusterInvokerMap.computeIfAbsent(cluster,
            (key) -> (AbstractClusterInvoker<T>) builder.apply(finalCluster).join(directory, false));
        return abstractClusterInvoker.doInvoke(invocation, invokers, loadbalance);
    }

}
