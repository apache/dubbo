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
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ReflectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.TestMetricsInvoker;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.event.MetricsDispatcher;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.filter.MetricsFilter;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.model.MetricsCategory.QPS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AggregateMetricsCollectorTest {

    private ApplicationModel applicationModel;
    private DefaultMetricsCollector defaultCollector;
    private String interfaceName;
    private String methodName;
    private String group;
    private String version;
    private RpcInvocation invocation;
    private String side;
    private MetricsDispatcher metricsDispatcher;
    private AggregateMetricsCollector collector;
    private MetricsFilter metricsFilter;

    public MethodMetric getTestMethodMetric() {

        MethodMetric methodMetric =
                new MethodMetric(applicationModel, invocation, MethodMetric.isServiceLevel(applicationModel));
        methodMetric.setGroup("TestGroup");
        methodMetric.setVersion("1.0.0");
        methodMetric.setSide("PROVIDER");

        return methodMetric;
    }

    @BeforeEach
    public void setup() {
        applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");
        applicationModel.getApplicationConfigManager().setApplication(config);

        MetricsConfig metricsConfig = new MetricsConfig();
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        aggregationConfig.setBucketNum(12);
        aggregationConfig.setTimeWindowSeconds(120);
        metricsConfig.setAggregation(aggregationConfig);
        applicationModel.getApplicationConfigManager().setMetrics(metricsConfig);
        metricsDispatcher = applicationModel.getBeanFactory().getOrRegisterBean(MetricsDispatcher.class);
        defaultCollector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        collector = applicationModel.getBeanFactory().getOrRegisterBean(AggregateMetricsCollector.class);
        collector.setCollectEnabled(true);

        defaultCollector = new DefaultMetricsCollector(applicationModel);
        defaultCollector.setCollectEnabled(true);

        metricsFilter = new MetricsFilter();
        metricsFilter.setApplicationModel(applicationModel);

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
    }

    @Test
    void testListener() {
        AggregateMetricsCollector metricsCollector = new AggregateMetricsCollector(applicationModel);
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

    @Test
    void testRequestsMetrics() {
        String applicationName = applicationModel.getApplicationName();

        defaultCollector.setApplicationName(applicationName);

        metricsFilter.invoke(new TestMetricsInvoker(side), invocation);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AppResponse mockRpcResult = new AppResponse();
        mockRpcResult.setException(new RpcException(RpcException.NETWORK_EXCEPTION));
        Result result = AsyncRpcResult.newDefaultAsyncResult(mockRpcResult, invocation);
        metricsFilter.onResponse(result, new TestMetricsInvoker(side), invocation);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
        }

        samples = collector.collect();

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = samples.stream()
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_NETWORK_FAILED_AGG.getNameByType(side)), 1L);
        Assertions.assertTrue(sampleMap.containsKey(MetricsKey.METRIC_QPS.getNameByType(side)));
    }

    @Test
    public void testQPS() {
        ApplicationModel applicationModel = mock(ApplicationModel.class);
        ConfigManager configManager = mock(ConfigManager.class);
        MetricsConfig metricsConfig = mock(MetricsConfig.class);
        ScopeBeanFactory beanFactory = mock(ScopeBeanFactory.class);
        AggregationConfig aggregationConfig = mock(AggregationConfig.class);

        when(applicationModel.getApplicationConfigManager()).thenReturn(configManager);
        when(applicationModel.getBeanFactory()).thenReturn(beanFactory);
        DefaultMetricsCollector defaultMetricsCollector = new DefaultMetricsCollector(applicationModel);
        when(beanFactory.getBean(DefaultMetricsCollector.class)).thenReturn(defaultMetricsCollector);
        when(configManager.getMetrics()).thenReturn(Optional.of(metricsConfig));
        when(metricsConfig.getAggregation()).thenReturn(aggregationConfig);
        when(aggregationConfig.getEnabled()).thenReturn(Boolean.TRUE);

        AggregateMetricsCollector collector = new AggregateMetricsCollector(applicationModel);

        MethodMetric methodMetric = getTestMethodMetric();

        TimeWindowCounter qpsCounter = new TimeWindowCounter(10, 120);

        for (int i = 0; i < 10000; i++) {
            qpsCounter.increment();
        }

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<MethodMetric, TimeWindowCounter> qps =
                (ConcurrentHashMap<MethodMetric, TimeWindowCounter>) ReflectionUtils.getField(collector, "qps");
        qps.put(methodMetric, qpsCounter);

        List<MetricSample> collectedQPS = new ArrayList<>();
        ReflectionUtils.invoke(collector, "collectQPS", collectedQPS);

        Assertions.assertFalse(collectedQPS.isEmpty());
        Assertions.assertEquals(1, collectedQPS.size());

        MetricSample sample = collectedQPS.get(0);
        Assertions.assertEquals(MetricsKey.METRIC_QPS.getNameByType("PROVIDER"), sample.getName());
        Assertions.assertEquals(MetricsKey.METRIC_QPS.getDescription(), sample.getDescription());

        Assertions.assertEquals(QPS, sample.getCategory());
        Assertions.assertEquals(10000, ((TimeWindowCounter) ((GaugeMetricSample<?>) sample).getValue()).get());
    }

    @Test
    public void testRtAggregation() {
        metricsDispatcher.addListener(collector);
        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        MetricsConfig config = configManager.getMetrics().orElse(null);
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        config.setAggregation(aggregationConfig);

        List<Long> rtList = new ArrayList<>();
        rtList.add(10L);
        rtList.add(20L);
        rtList.add(30L);

        for (Long requestTime : rtList) {
            RequestEvent requestEvent = RequestEvent.toRequestEvent(
                    applicationModel,
                    null,
                    null,
                    null,
                    invocation,
                    MetricsSupport.getSide(invocation),
                    MethodMetric.isServiceLevel(applicationModel));
            TestRequestEvent testRequestEvent =
                    new TestRequestEvent(requestEvent.getSource(), requestEvent.getTypeWrapper());
            testRequestEvent.putAttachment(MetricsConstants.INVOCATION, invocation);
            testRequestEvent.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
            testRequestEvent.putAttachment(MetricsConstants.INVOCATION_SIDE, MetricsSupport.getSide(invocation));
            testRequestEvent.setRt(requestTime);
            MetricsEventBus.post(testRequestEvent, () -> null);
        }

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            GaugeMetricSample gaugeMetricSample = (GaugeMetricSample<?>) sample;

            if (gaugeMetricSample.getName().endsWith("max.milliseconds.aggregate")) {
                Assertions.assertEquals(30, gaugeMetricSample.applyAsDouble());
            }
            if (gaugeMetricSample.getName().endsWith("min.milliseconds.aggregate")) {
                Assertions.assertEquals(10L, gaugeMetricSample.applyAsDouble());
            }

            if (gaugeMetricSample.getName().endsWith("avg.milliseconds.aggregate")) {
                Assertions.assertEquals(20L, gaugeMetricSample.applyAsDouble());
            }
        }
    }

    @Test
    void testP95AndP99() throws InterruptedException {

        metricsDispatcher.addListener(collector);
        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        MetricsConfig config = configManager.getMetrics().orElse(null);
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        config.setAggregation(aggregationConfig);

        List<Long> requestTimes = new ArrayList<>(10000);

        for (int i = 0; i < 300; i++) {
            requestTimes.add(Double.valueOf(1000 * Math.random()).longValue());
        }

        Collections.sort(requestTimes);
        double p95Index = 0.95 * (requestTimes.size() - 1);
        double p99Index = 0.99 * (requestTimes.size() - 1);

        double manualP95 = requestTimes.get((int) Math.round(p95Index));
        double manualP99 = requestTimes.get((int) Math.round(p99Index));

        for (Long requestTime : requestTimes) {
            RequestEvent requestEvent = RequestEvent.toRequestEvent(
                    applicationModel,
                    null,
                    null,
                    null,
                    invocation,
                    MetricsSupport.getSide(invocation),
                    MethodMetric.isServiceLevel(applicationModel));
            TestRequestEvent testRequestEvent =
                    new TestRequestEvent(requestEvent.getSource(), requestEvent.getTypeWrapper());
            testRequestEvent.putAttachment(MetricsConstants.INVOCATION, invocation);
            testRequestEvent.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
            testRequestEvent.putAttachment(MetricsConstants.INVOCATION_SIDE, MetricsSupport.getSide(invocation));
            testRequestEvent.setRt(requestTime);
            MetricsEventBus.post(testRequestEvent, () -> null);
        }
        Thread.sleep(4000L);

        List<MetricSample> samples = collector.collect();

        GaugeMetricSample<?> p95Sample = samples.stream()
                .filter(sample -> sample.getName().endsWith("p95"))
                .map(sample -> (GaugeMetricSample<?>) sample)
                .findFirst()
                .orElse(null);

        GaugeMetricSample<?> p99Sample = samples.stream()
                .filter(sample -> sample.getName().endsWith("p99"))
                .map(sample -> (GaugeMetricSample<?>) sample)
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(p95Sample);
        Assertions.assertNotNull(p99Sample);

        double p95 = p95Sample.applyAsDouble();
        double p99 = p99Sample.applyAsDouble();

        // An error of less than 5% is allowed
        System.out.println(Math.abs(1 - p95 / manualP95));
        Assertions.assertTrue(Math.abs(1 - p95 / manualP95) < 0.05);
        Assertions.assertTrue(Math.abs(1 - p99 / manualP99) < 0.05);
    }

    @Test
    void testGenericCache() {
        List<Class<?>> classGenerics =
                ReflectionUtils.getClassGenerics(AggregateMetricsCollector.class, MetricsListener.class);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(classGenerics));
        Assertions.assertEquals(RequestEvent.class, classGenerics.get(0));
    }

    public static class TestRequestEvent extends RequestEvent {
        private long rt;

        public TestRequestEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
            super(applicationModel, null, null, null, typeWrapper);
        }

        public void setRt(long rt) {
            this.rt = rt;
        }

        @Override
        public TimePair getTimePair() {
            return new TestTimePair(rt);
        }
    }

    public static class TestTimePair extends TimePair {

        long rt;

        public TestTimePair(long rt) {
            super(rt);
            this.rt = rt;
        }

        @Override
        public long calc() {
            return this.rt;
        }
    }
}
