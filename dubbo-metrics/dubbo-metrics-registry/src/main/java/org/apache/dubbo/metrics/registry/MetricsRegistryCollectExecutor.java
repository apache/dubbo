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

package org.apache.dubbo.metrics.registry;

import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.MetricsConstants.METRIC_FILTER_START_TIME;

public class MetricsRegistryCollectExecutor {


    public static void postExecute(String applicationName, RegistryMetricsCollector collector, Invocation invocation, Result result) {
        if (result.hasException()) {
            throwExecute(applicationName, collector, invocation, result.getException());
            return;
        }
//        collector.increaseSucceedRequests(applicationName, invocation);
        endExecute(applicationName, collector, invocation);
    }

    public static void throwExecute(String applicationName, RegistryMetricsCollector collector, Invocation invocation, Throwable throwable) {
//        if (throwable instanceof RpcException) {
//            RpcException rpcException = (RpcException) throwable;
//            switch (rpcException.getCode()) {
//
//                case RpcException.TIMEOUT_EXCEPTION:
//                    collector.timeoutRequests(applicationName, invocation);
//                    break;
//
//                case RpcException.LIMIT_EXCEEDED_EXCEPTION:
//                    collector.limitRequests(applicationName, invocation);
//                    break;
//
//                case RpcException.BIZ_EXCEPTION:
//                    collector.businessFailedRequests(applicationName, invocation);
//                    break;
//
//                default:
//                    collector.increaseUnknownFailedRequests(applicationName, invocation);
//            }
//        }
//
//        collector.totalFailedRequests(applicationName, invocation);

        endExecute(applicationName, collector, invocation, () -> throwable instanceof RpcException && ((RpcException) throwable).isBiz());
    }

    private static void endExecute(String applicationName, RegistryMetricsCollector collector, Invocation invocation) {
        endExecute(applicationName, collector, invocation, () -> true);
    }

    private static void endExecute(String applicationName, RegistryMetricsCollector collector, Invocation invocation, Supplier<Boolean> rtStat) {
        if (rtStat.get()) {
            Long endTime = System.currentTimeMillis();
            Long beginTime = (Long) invocation.get(METRIC_FILTER_START_TIME);
            Long rt = endTime - beginTime;
            collector.addRT(applicationName, rt);
        }
//        collector.decreaseProcessingRequests(applicationName, invocation);
    }

}
