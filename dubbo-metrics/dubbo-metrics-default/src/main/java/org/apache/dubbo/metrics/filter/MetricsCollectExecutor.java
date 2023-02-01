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

package org.apache.dubbo.metrics.filter;

import java.util.function.Supplier;

import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import static org.apache.dubbo.common.constants.MetricsConstants.METRIC_FILTER_START_TIME;
import static org.apache.dubbo.rpc.support.RpcUtils.isGenericCall;

public class MetricsCollectExecutor {

    public static void beforeExecute(DefaultMetricsCollector collector, Invocation invocation) {
        collector.increaseTotalRequests(invocation);
        collector.increaseProcessingRequests(invocation);
        invocation.put(METRIC_FILTER_START_TIME, System.currentTimeMillis());
    }

    public static void postExecute(DefaultMetricsCollector collector, Invocation invocation, Result result) {
        if (result.hasException()) {
            throwExecute(collector, invocation, result.getException());
            return;
        }
        collector.increaseSucceedRequests(invocation);
        endExecute(collector, invocation);
    }

    public static void throwExecute(DefaultMetricsCollector collector, Invocation invocation, Throwable throwable) {
        if (throwable instanceof RpcException) {
            RpcException rpcException = (RpcException) throwable;
            switch (rpcException.getCode()) {

                case RpcException.TIMEOUT_EXCEPTION:
                    collector.timeoutRequests(invocation);
                    break;

                case RpcException.LIMIT_EXCEEDED_EXCEPTION:
                    collector.limitRequests(invocation);
                    break;

                case RpcException.BIZ_EXCEPTION:
                    collector.businessFailedRequests(invocation);
                    break;

                default:
                    collector.increaseUnknownFailedRequests(invocation);
            }
        }

        collector.totalFailedRequests(invocation);

        endExecute(collector, invocation, () -> throwable instanceof RpcException && ((RpcException) throwable).isBiz());
    }

    private static void endExecute(DefaultMetricsCollector collector, Invocation invocation) {
        endExecute(collector, invocation, () -> true);
    }

    private static void endExecute(DefaultMetricsCollector collector, Invocation invocation, Supplier<Boolean> rtStat) {
        if (rtStat.get()) {
            Long endTime = System.currentTimeMillis();
            Long beginTime = (Long) invocation.get(METRIC_FILTER_START_TIME);
            Long rt = endTime - beginTime;
            collector.addRT(invocation, rt);
        }
        collector.decreaseProcessingRequests(invocation);
    }

}
