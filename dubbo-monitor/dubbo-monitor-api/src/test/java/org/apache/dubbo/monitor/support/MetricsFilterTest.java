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
import com.alibaba.metrics.IMetricManager;
import com.alibaba.metrics.MetricLevel;
import com.alibaba.metrics.MetricManager;
import com.alibaba.metrics.MetricName;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.monitor.service.DemoService;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class MetricsFilterTest {

    private final Invoker<DemoService> serviceInvoker = new Invoker<DemoService>() {
        @Override
        public Class<DemoService> getInterface() {
            return DemoService.class;
        }

        public URL getUrl() {
            return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/org.apache.dubbo.monitor.service.DemoService");
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        public Result invoke(Invocation invocation) throws RpcException {
            return null;
        }

        @Override
        public void destroy() {
        }
    };

    private final Invoker<DemoService> timeoutInvoker = new Invoker<DemoService>() {
        @Override
        public Class<DemoService> getInterface() {
            return DemoService.class;
        }

        public URL getUrl() {
            return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/org.apache.dubbo.monitor.service.DemoService");
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        public Result invoke(Invocation invocation) throws RpcException {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION);
        }

        @Override
        public void destroy() {
        }
    };

    @Test
    public void testConsumerSuccess() throws Exception {
        IMetricManager metricManager = MetricManager.getIMetricManager();
        metricManager.clear();
        MetricsFilter metricsFilter = new MetricsFilter();
        Invocation invocation = new RpcInvocation("sayName", new Class<?>[0], new Object[0]);
        RpcContext.getContext().setRemoteAddress(NetUtils.getLocalHost(), 20880).setLocalAddress(NetUtils.getLocalHost(), 2345);
        RpcContext.getContext().setUrl(serviceInvoker.getUrl().addParameter(Constants.SIDE_KEY, Constants.CONSUMER_SIDE));
        for (int i = 0; i < 100; i++) {
            metricsFilter.invoke(serviceInvoker, invocation);
        }
        FastCompass dubboClient = metricManager.getFastCompass(Constants.DUBBO_GROUP, new MetricName(Constants.DUBBO_CONSUMER, MetricLevel.MAJOR));
        FastCompass dubboMethod = metricManager.getFastCompass(Constants.DUBBO_GROUP, new MetricName(Constants.DUBBO_CONSUMER_METHOD, new HashMap<String, String>(4) {
            {
                put(Constants.SERVICE, "org.apache.dubbo.monitor.service.DemoService");
                put(Constants.METHOD, "sayName");
            }
        }, MetricLevel.NORMAL));
        long timestamp = System.currentTimeMillis() / 5000 * 5000;
        Assertions.assertEquals(100, dubboClient.getMethodCountPerCategory(0).get("success").get(timestamp));
        timestamp = timestamp / 15000 * 15000;
        Assertions.assertEquals(100, dubboMethod.getMethodCountPerCategory(0).get("success").get(timestamp));

    }

    @Test
    public void testConsumerTimeout() {
        IMetricManager metricManager = MetricManager.getIMetricManager();
        metricManager.clear();
        MetricsFilter metricsFilter = new MetricsFilter();
        Invocation invocation = new RpcInvocation("timeoutException", null, null);
        RpcContext.getContext().setRemoteAddress(NetUtils.getLocalHost(), 20880).setLocalAddress(NetUtils.getLocalHost(), 2345);
        RpcContext.getContext().setUrl(timeoutInvoker.getUrl().addParameter(Constants.SIDE_KEY, Constants.CONSUMER_SIDE)
                                                              .addParameter(Constants.TIMEOUT_KEY, 300));
        for (int i = 0; i < 10; i++) {
            try {
                metricsFilter.invoke(timeoutInvoker, invocation);
            } catch (RpcException e) {
                //ignore
            }
        }
        FastCompass dubboClient = metricManager.getFastCompass(Constants.DUBBO_GROUP, new MetricName(Constants.DUBBO_CONSUMER, MetricLevel.MAJOR));
        FastCompass dubboMethod = metricManager.getFastCompass(Constants.DUBBO_GROUP, new MetricName(Constants.DUBBO_CONSUMER_METHOD, new HashMap<String, String>(4) {
            {
                put(Constants.SERVICE, "org.apache.dubbo.monitor.service.DemoService");
                put(Constants.METHOD, "timeoutException");
            }
        }, MetricLevel.NORMAL));
        long timestamp = System.currentTimeMillis() / 5000 * 5000;
        Assertions.assertEquals(10, dubboClient.getMethodCountPerCategory(0).get("timeoutError").get(timestamp));
        timestamp = timestamp / 15000 * 15000;
        Assertions.assertEquals(10, dubboMethod.getMethodCountPerCategory(0).get("timeoutError").get(timestamp));

    }

    @Test
    public void testProviderSuccess() throws Exception {
        IMetricManager metricManager = MetricManager.getIMetricManager();
        metricManager.clear();
        MetricsFilter metricsFilter = new MetricsFilter();
        Invocation invocation = new RpcInvocation("sayName", new Class<?>[0], new Object[0]);
        RpcContext.getContext().setRemoteAddress(NetUtils.getLocalHost(), 20880).setLocalAddress(NetUtils.getLocalHost(), 2345);
        RpcContext.getContext().setUrl(serviceInvoker.getUrl().addParameter(Constants.SIDE_KEY, Constants.PROVIDER));
        for (int i = 0; i < 100; i++) {
            metricsFilter.invoke(serviceInvoker, invocation);
        }
        FastCompass dubboClient = metricManager.getFastCompass(Constants.DUBBO_GROUP, new MetricName(Constants.DUBBO_PROVIDER, MetricLevel.MAJOR));
        FastCompass dubboMethod = metricManager.getFastCompass(Constants.DUBBO_GROUP, new MetricName(Constants.DUBBO_PROVIDER_METHOD, new HashMap<String, String>(4) {
            {
                put(Constants.SERVICE, "org.apache.dubbo.monitor.service.DemoService");
                put(Constants.METHOD, "sayName");
            }
        }, MetricLevel.NORMAL));
        long timestamp = System.currentTimeMillis() / 5000 * 5000;
        Assertions.assertEquals(100, dubboClient.getMethodCountPerCategory(0).get("success").get(timestamp));
        timestamp = timestamp / 15000 * 15000;
        Assertions.assertEquals(100, dubboMethod.getMethodCountPerCategory(0).get("success").get(timestamp));
    }
}
