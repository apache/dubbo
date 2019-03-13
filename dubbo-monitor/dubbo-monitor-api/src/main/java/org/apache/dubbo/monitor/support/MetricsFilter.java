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
package org.apache.dubbo.monitor.support;

import com.alibaba.metrics.FastCompass;
import com.alibaba.metrics.MetricLevel;
import com.alibaba.metrics.MetricManager;
import com.alibaba.metrics.MetricName;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.HashMap;

public class MetricsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(MetricsFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext context = RpcContext.getContext();
        boolean isProvider = context.isProviderSide();
        long start = System.currentTimeMillis();
        try {
            Result result = invoker.invoke(invocation); // proceed invocation chain
            long duration = System.currentTimeMillis() - start;
            reportMetrics(invoker, invocation, duration, "success", isProvider);
            return result;
        } catch (RpcException e) {
            long duration = System.currentTimeMillis() - start;
            String result = "error";
            if (e.isTimeout()) {
                result = "timeoutError";
            }
            if (e.isBiz()) {
                result = "bisError";
            }
            if (e.isNetwork()) {
                result = "networkError";
            }
            if (e.isSerialization()) {
                result = "serializationError";
            }
            reportMetrics(invoker, invocation, duration, result, isProvider);
            throw e;
        }
    }

    private void reportMetrics(Invoker<?> invoker, Invocation invocation, long duration, String result, boolean isProvider) {
        String serviceName = invoker.getInterface().getName();
        String methodName = RpcUtils.getMethodName(invocation);
        MetricName global;
        MetricName method;
        if (isProvider) {
            global = new MetricName(Constants.DUBBO_PROVIDER, MetricLevel.MAJOR);
            method = new MetricName(Constants.DUBBO_PROVIDER_METHOD, new HashMap<String, String>(4) {
                {
                    put(Constants.SERVICE, serviceName);
                    put(Constants.METHOD, methodName);
                }
            }, MetricLevel.NORMAL);
        } else {
            global = new MetricName(Constants.DUBBO_CONSUMER, MetricLevel.MAJOR);
            method = new MetricName(Constants.DUBBO_CONSUMER_METHOD, new HashMap<String, String>(4) {
                {
                    put(Constants.SERVICE, serviceName);
                    put(Constants.METHOD, methodName);
                }
            }, MetricLevel.NORMAL);
        }
        setCompassQuantity(Constants.DUBBO_GROUP, result, duration, global, method);
    }

    private void setCompassQuantity(String groupName, String result, long duration, MetricName... metricNames) {
        for (MetricName metricName : metricNames) {
            FastCompass compass = MetricManager.getFastCompass(groupName, metricName);
            compass.record(duration, result);
        }
    }

}
