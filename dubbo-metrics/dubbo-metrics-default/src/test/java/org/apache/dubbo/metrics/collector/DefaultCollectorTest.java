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
package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.TestMetricsInvoker;
import org.apache.dubbo.metrics.event.MetricsDispatcher;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.filter.MetricsFilter;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.metrics.DefaultConstants.METRIC_FILTER_EVENT;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_PROCESSING;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_TIMEOUT;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_TOTAL_FAILED;

class DefaultCollectorTest {

    private ApplicationModel applicationModel;

    private String interfaceName;
    private String methodName;
    private String group;
    private String version;
    private RpcInvocation invocation;
    private String side;

    MetricsDispatcher metricsDispatcher;
    DefaultMetricsCollector defaultCollector;

    MetricsFilter metricsFilter;

    @BeforeEach
    public void setup() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel.getApplicationConfigManager().setApplication(config);
        metricsDispatcher = applicationModel.getBeanFactory().getOrRegisterBean(MetricsDispatcher.class);
        defaultCollector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        defaultCollector.setCollectEnabled(true);

        interfaceName = "org.apache.dubbo.MockInterface";
        methodName = "mockMethod";
        group = "mockGroup";
        version = "1.0.0";
        invocation = new RpcInvocation(methodName, interfaceName, "serviceKey", null, null);
        invocation.setTargetServiceUniqueName(group + "/" + interfaceName + ":" + version);
        invocation.setAttachment(GROUP_KEY, group);
        invocation.setAttachment(VERSION_KEY, version);
        side = CommonConstants.CONSUMER;
        invocation.setInvoker(new TestMetricsInvoker(side));
        invocation.setTargetServiceUniqueName(group + "/" + interfaceName + ":" + version);
        RpcContext.getServiceContext()
                .setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=" + side));

        metricsFilter = new MetricsFilter();
        metricsFilter.setApplicationModel(applicationModel);
    }

    @Test
    void testListener() {
        DefaultMetricsCollector metricsCollector = new DefaultMetricsCollector(applicationModel);
        RequestEvent event = RequestEvent.toRequestEvent(
                applicationModel,
                null,
                null,
                null,
                invocation,
                MetricsSupport.getSide(invocation),
                MethodMetric.isServiceLevel(applicationModel));
        RequestEvent beforeEvent = RequestEvent.toRequestErrorEvent(
                applicationModel,
                null,
                null,
                invocation,
                MetricsSupport.getSide(invocation),
                RpcException.FORBIDDEN_EXCEPTION,
                MethodMetric.isServiceLevel(applicationModel));

        Assertions.assertTrue(metricsCollector.isSupport(event));
        Assertions.assertTrue(metricsCollector.isSupport(beforeEvent));
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    /**
     * No rt metrics because Aggregate calc
     */
    @Test
    void testRequestEventNoRt() {

        applicationModel.getBeanFactory().getOrRegisterBean(MetricsDispatcher.class);
        DefaultMetricsCollector collector =
                applicationModel.getBeanFactory().getOrRegisterBean(DefaultMetricsCollector.class);
        collector.setCollectEnabled(true);

        metricsFilter.invoke(new TestMetricsInvoker(side), invocation);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AppResponse mockRpcResult = new AppResponse();
        //        mockRpcResult.setException(new RpcException("hessian"));
        Result result = AsyncRpcResult.newDefaultAsyncResult(mockRpcResult, invocation);
        metricsFilter.onResponse(result, new TestMetricsInvoker(side), invocation);

        RequestEvent eventObj = (RequestEvent) invocation.get(METRIC_FILTER_EVENT);
        long c1 = eventObj.getTimePair().calc();

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        // num(total+success+processing) + rt(5) = 8
        Assertions.assertEquals(8, metricSamples.size());
        List<String> metricsNames =
                metricSamples.stream().map(MetricSample::getName).collect(Collectors.toList());
        // No error will contain total+success+processing
        String REQUESTS =
                new MetricsKeyWrapper(METRIC_REQUESTS, MetricsPlaceValue.of(side, MetricsLevel.SERVICE)).targetKey();
        String SUCCEED = new MetricsKeyWrapper(
                        METRIC_REQUESTS_SUCCEED, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                .targetKey();
        String PROCESSING = new MetricsKeyWrapper(
                        METRIC_REQUESTS_PROCESSING, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                .targetKey();
        Assertions.assertTrue(metricsNames.contains(REQUESTS));
        Assertions.assertTrue(metricsNames.contains(SUCCEED));
        Assertions.assertTrue(metricsNames.contains(PROCESSING));
        for (MetricSample metricSample : metricSamples) {
            if (metricSample instanceof GaugeMetricSample) {
                GaugeMetricSample<?> gaugeMetricSample = (GaugeMetricSample<?>) metricSample;
                Object objVal = gaugeMetricSample.getValue();
                if (objVal instanceof Map) {
                    Map<ServiceKeyMetric, AtomicLong> value = (Map<ServiceKeyMetric, AtomicLong>) objVal;
                    if (metricSample.getName().equals(REQUESTS)) {
                        Assertions.assertTrue(
                                value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 1));
                    }
                    if (metricSample.getName().equals(PROCESSING)) {
                        Assertions.assertTrue(
                                value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 0));
                    }
                }
            } else {
                AtomicLong value = (AtomicLong) ((CounterMetricSample<?>) metricSample).getValue();
                if (metricSample.getName().equals(SUCCEED)) {
                    Assertions.assertEquals(1, value.intValue());
                }
            }
        }

        metricsFilter.invoke(new TestMetricsInvoker(side), invocation);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        metricsFilter.onError(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout"), new TestMetricsInvoker(side), invocation);
        eventObj = (RequestEvent) invocation.get(METRIC_FILTER_EVENT);
        long c2 = eventObj.getTimePair().calc();
        metricSamples = collector.collect();

        // num(total+success+error+total_error+processing) + rt(5) = 5
        Assertions.assertEquals(10, metricSamples.size());

        String TIMEOUT = new MetricsKeyWrapper(
                        METRIC_REQUESTS_TIMEOUT, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                .targetKey();
        String TOTAL_FAILED = new MetricsKeyWrapper(
                        METRIC_REQUESTS_TOTAL_FAILED, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                .targetKey();
        for (MetricSample metricSample : metricSamples) {
            if (metricSample instanceof GaugeMetricSample) {
                GaugeMetricSample<?> gaugeMetricSample = (GaugeMetricSample<?>) metricSample;
                Object objVal = gaugeMetricSample.getValue();
                if (objVal instanceof Map) {
                    Map<ServiceKeyMetric, AtomicLong> value =
                            (Map<ServiceKeyMetric, AtomicLong>) ((GaugeMetricSample<?>) metricSample).getValue();
                    if (metricSample.getName().equals(REQUESTS)) {
                        Assertions.assertTrue(
                                value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 2));
                    }
                    if (metricSample.getName().equals(REQUESTS)) {
                        Assertions.assertTrue(
                                value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 2));
                    }
                    if (metricSample.getName().equals(PROCESSING)) {
                        Assertions.assertTrue(
                                value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 0));
                    }
                    if (metricSample.getName().equals(TIMEOUT)) {
                        Assertions.assertTrue(
                                value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 1));
                    }
                    if (metricSample.getName().equals(TOTAL_FAILED)) {
                        Assertions.assertTrue(
                                value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 1));
                    }
                }
            } else {
                AtomicLong value = (AtomicLong) ((CounterMetricSample<?>) metricSample).getValue();
                if (metricSample.getName().equals(SUCCEED)) {
                    Assertions.assertEquals(1, value.intValue());
                }
            }
        }

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        Map<String, Long> sampleMap = metricSamples.stream()
                .filter(metricSample -> metricSample instanceof GaugeMetricSample)
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(
                                MetricsKey.METRIC_RT_LAST, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                        .targetKey()),
                c2);
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(
                                MetricsKey.METRIC_RT_MIN, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                        .targetKey()),
                Math.min(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(
                                MetricsKey.METRIC_RT_MAX, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                        .targetKey()),
                Math.max(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(
                                MetricsKey.METRIC_RT_AVG, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                        .targetKey()),
                (c1 + c2) / 2);
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(
                                MetricsKey.METRIC_RT_SUM, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                        .targetKey()),
                c1 + c2);
    }
}
