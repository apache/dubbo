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
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * When fails, record failure requests and schedule for retry on a regular interval.
 * Especially useful for services of notification.
 *
 * <a href="http://en.wikipedia.org/wiki/Failback">Failback</a>
 */
public class FailbackClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailbackClusterInvoker.class);

    /**
     * 重试频率
     */
    private static final long RETRY_FAILED_PERIOD = 5 * 1000;

    /**
     * ScheduledExecutorService 对象
     */
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, new NamedThreadFactory("failback-cluster-timer", true));
    /**
     * 失败任务集合
     */
    private final ConcurrentMap<Invocation, AbstractClusterInvoker<?>> failed = new ConcurrentHashMap<Invocation, AbstractClusterInvoker<?>>();
    /**
     * 重试任务 Future
     */
    private volatile ScheduledFuture<?> retryFuture;

    public FailbackClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    private void addFailed(Invocation invocation, AbstractClusterInvoker<?> router) {
        // 若定时任务未初始化，进行创建
        if (retryFuture == null) {
            synchronized (this) {
                if (retryFuture == null) {
                    retryFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {

                        public void run() {
                            // collect retry statistics
                            try {
                                retryFailed();
                            } catch (Throwable t) { // Defensive fault tolerance
                                logger.error("Unexpected error occur at collect statistic", t);
                            }
                        }
                    }, RETRY_FAILED_PERIOD, RETRY_FAILED_PERIOD, TimeUnit.MILLISECONDS);
                }
            }
        }
        // 添加到失败任务
        failed.put(invocation, router);
    }

    void retryFailed() {
        if (failed.size() == 0) {
            return;
        }
        // 循环重试任务，逐个调用
        for (Map.Entry<Invocation, AbstractClusterInvoker<?>> entry : new HashMap<Invocation, AbstractClusterInvoker<?>>(failed).entrySet()) { // 创建集合
            Invocation invocation = entry.getKey();
            Invoker<?> invoker = entry.getValue();
            try {
                // RPC 调用得到 Result
                invoker.invoke(invocation);
                // 移除失败任务
                failed.remove(invocation);
            } catch (Throwable e) {
                logger.error("Failed retry to invoke method " + invocation.getMethodName() + ", waiting again.", e);
            }
        }
    }

    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        try {
            // 检查 invokers 即可用Invoker集合是否为空，如果为空，那么抛出异常
            checkInvokers(invokers, invocation);
            // 根据负载均衡机制从 invokers 中选择一个Invoker
            Invoker<T> invoker = select(loadbalance, invocation, invokers, null);
            // RPC 调用得到 Result
            return invoker.invoke(invocation);
        } catch (Throwable e) {
            logger.error("Failback to invoke method " + invocation.getMethodName() + ", wait for retry in background. Ignored exception: " + e.getMessage() + ", ", e);
            // 添加到失败任务
            addFailed(invocation, this);
            return new RpcResult(); // ignore
        }
    }

}