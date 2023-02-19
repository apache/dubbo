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

import org.apache.dubbo.metrics.collector.sample.MethodMetricsSampler;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.MetricsConstants.METRIC_FILTER_START_TIME;

public class MethodMetricsInterceptor {

    private MethodMetricsSampler sampler;

    public MethodMetricsInterceptor(MethodMetricsSampler sampler) {
        this.sampler = sampler;
    }

    public void beforeExecute(Invocation invocation) {
        sampler.incOnEvent(invocation, MetricsEvent.Type.TOTAL);
        sampler.incOnEvent(invocation,MetricsEvent.Type.PROCESSING);
        invocation.put(METRIC_FILTER_START_TIME, System.currentTimeMillis());
    }

    public void postExecute(Invocation invocation, Result result) {
        if (result.hasException()) {
            throwExecute(invocation, result.getException());
            return;
        }
        sampler.incOnEvent(invocation,MetricsEvent.Type.SUCCEED);
        endExecute(invocation);
    }

    public void throwExecute(Invocation invocation, Throwable throwable) {
        if (throwable instanceof RpcException) {
            RpcException rpcException = (RpcException) throwable;
            switch (rpcException.getCode()) {

                case RpcException.TIMEOUT_EXCEPTION:
                    sampler.incOnEvent(invocation,MetricsEvent.Type.REQUEST_TIMEOUT);
                    break;

                case RpcException.LIMIT_EXCEEDED_EXCEPTION:
                    sampler.incOnEvent(invocation,MetricsEvent.Type.REQUEST_LIMIT);
                    break;

                case RpcException.BIZ_EXCEPTION:
                    sampler.incOnEvent(invocation,MetricsEvent.Type.BUSINESS_FAILED);
                    break;

                default:
                    sampler.incOnEvent(invocation,MetricsEvent.Type.UNKNOWN_FAILED);
            }
        }

        sampler.incOnEvent(invocation,MetricsEvent.Type.TOTAL_FAILED);

        endExecute(invocation, () -> throwable instanceof RpcException && ((RpcException) throwable).isBiz());
    }

    private void endExecute(Invocation invocation) {
        endExecute(invocation, () -> true);
    }

    private void endExecute(Invocation invocation, Supplier<Boolean> rtStat) {
        if (rtStat.get()) {
            Long endTime = System.currentTimeMillis();
            Long beginTime = (Long) invocation.get(METRIC_FILTER_START_TIME);
            Long rt = endTime - beginTime;
            sampler.addRT(invocation, rt);
        }
        sampler.dec(invocation,MetricsEvent.Type.PROCESSING);
    }

}
