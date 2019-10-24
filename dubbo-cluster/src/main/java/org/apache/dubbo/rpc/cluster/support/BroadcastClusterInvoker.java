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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * BroadcastClusterInvoker
 */
public class BroadcastClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastClusterInvoker.class);

    private ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("broadcast-cluster-executor", true));

    public BroadcastClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(final Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        checkInvokers(invokers, invocation);
        RpcContext.getContext().setInvokers((List) invokers);

        List<Callable<Result>> tasks = IntStream.range(0, invokers.size()).mapToObj(index -> {
            Callable<Result> callable = () -> invokers.get(index).invoke(invocation);
            return callable;
        }).collect(Collectors.toList());
        try {
            List<Future<Result>> futureList = executor.invokeAll(tasks);
            futureList.stream().map(it -> {
                try {
                    return it.get();
                } catch (Throwable e) {
                    return e;
                }
            }).filter(it -> it instanceof Throwable).findFirst().ifPresent(it -> {
                Throwable ex = (Throwable) it;
                logger.warn(ex.getMessage(), ex);
                throw new RpcException(ex.getMessage(), ex);
            });
            int index = futureList.size() - 1 > 0 ? futureList.size() : 0;
            return futureList.get(index).get();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
            throw new RpcException(e.getMessage(), e);
        }
    }

}
