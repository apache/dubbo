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
import org.apache.dubbo.common.utils.WhiteBox;
import org.apache.dubbo.config.ApplicationConfig;

import org.apache.dubbo.config.MetricsConfig;

import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.TestMetricsInvoker;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.collector.sample.MethodMetricsSampler;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.*;
import static org.apache.dubbo.metrics.model.MetricsCategory.QPS;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

class AggregateMetricsCollectorTest {

    private ApplicationModel applicationModel;
    private DefaultMetricsCollector defaultCollector;
    private String interfaceName;
    private String methodName;
    private String group;
    private String version;
    private RpcInvocation invocation;
    private String side;

    public static MethodMetric getTestMethodMetric() {

        MethodMetric methodMetric = new MethodMetric();
        methodMetric.setApplicationName("TestApp");
        methodMetric.setInterfaceName("TestInterface");
        methodMetric.setMethodName("TestMethod");
        methodMetric.setGroup("TestGroup");
        methodMetric.setVersion("1.0.0");
        methodMetric.setSide("PROVIDER");

        return methodMetric;
    }

    public static AggregateMetricsCollector getTestCollector() {

        ApplicationModel applicationModel = mock(ApplicationModel.class);
        ConfigManager configManager = new ConfigManager(applicationModel);
        MetricsConfig metricsConfig = spy(new MetricsConfig());

        configManager.setMetrics(metricsConfig);

        when(metricsConfig.getAggregation()).thenReturn(new AggregationConfig());
        when(applicationModel.getApplicationConfigManager()).thenReturn(configManager);

        ScopeBeanFactory beanFactory = mock(ScopeBeanFactory.class);
        when(beanFactory.getBean(DefaultMetricsCollector.class)).thenReturn(new DefaultMetricsCollector());
        when(applicationModel.getBeanFactory()).thenReturn(beanFactory);

        return new AggregateMetricsCollector(applicationModel);
    }

    @BeforeEach
    public void setup() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);

        defaultCollector = new DefaultMetricsCollector();
        defaultCollector.setCollectEnabled(true);
        MetricsConfig metricsConfig = new MetricsConfig();
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        aggregationConfig.setBucketNum(12);
        aggregationConfig.setTimeWindowSeconds(120);
        metricsConfig.setAggregation(aggregationConfig);
        applicationModel.getApplicationConfigManager().setMetrics(metricsConfig);
        defaultCollector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);

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
        RpcContext.getServiceContext().setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=" + side));

    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testRequestsMetrics() {
        String applicationName = applicationModel.getApplicationName();
        AggregateMetricsCollector collector = new AggregateMetricsCollector(applicationModel);

        defaultCollector.setApplicationName(applicationName);
        MethodMetricsSampler methodMetricsCountSampler = defaultCollector.getMethodSampler();

        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.TOTAL.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.SUCCEED.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.UNKNOWN_FAILED.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.BUSINESS_FAILED.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.NETWORK_EXCEPTION.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.SERVICE_UNAVAILABLE.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.CODEC_EXCEPTION.getNameByType(side));


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
        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_TOTAL_AGG.getNameByType(side)), 1L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_SUCCEED_AGG.getNameByType(side)), 1L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_FAILED_AGG.getNameByType(side)), 1L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_BUSINESS_FAILED_AGG.getNameByType(side)), 1L);

        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_TOTAL_NETWORK_FAILED_AGG.getNameByType(side)), 1L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_TOTAL_CODEC_FAILED_AGG.getNameByType(side)), 1L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_TOTAL_SERVICE_UNAVAILABLE_FAILED_AGG.getNameByType(side)), 1L);

        Assertions.assertTrue(sampleMap.containsKey(MetricsKey.METRIC_QPS.getNameByType(side)));
    }

    @Test
    void testRTMetrics() {
        AggregateMetricsCollector collector = new AggregateMetricsCollector(applicationModel);

        defaultCollector.setApplicationName(applicationModel.getApplicationName());

        MethodMetricsSampler methodMetricsCountSampler = defaultCollector.getMethodSampler();

        methodMetricsCountSampler.addRT(invocation, 10L);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertTrue(sampleMap.containsKey(MetricsKey.METRIC_RT_P99.getNameByType(side)));
        Assertions.assertTrue(sampleMap.containsKey(MetricsKey.METRIC_RT_P95.getNameByType(side)));
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
        when(beanFactory.getBean(DefaultMetricsCollector.class)).thenReturn(new DefaultMetricsCollector());
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
        ConcurrentHashMap<MethodMetric, TimeWindowCounter> qps = (ConcurrentHashMap<MethodMetric, TimeWindowCounter>) WhiteBox.getField(collector, "qps");
        qps.put(methodMetric, qpsCounter);

        List<MetricSample> collectedQPS = new ArrayList<>();
        WhiteBox.invoke(collector, "collectQPS", collectedQPS);

        Assertions.assertFalse(collectedQPS.isEmpty());
        Assertions.assertEquals(1, collectedQPS.size());

        MetricSample sample = collectedQPS.get(0);
        Assertions.assertEquals(MetricsKey.METRIC_QPS.getNameByType("PROVIDER"), sample.getName());
        Assertions.assertEquals(MetricsKey.METRIC_QPS.getDescription(), sample.getDescription());

        Assertions.assertEquals(QPS, sample.getCategory());
        Assertions.assertEquals(10000, ((TimeWindowCounter) ((GaugeMetricSample<?>) sample).getValue()).get());
    }

    @Test
    void testP95AndP99() throws InterruptedException {
        AggregateMetricsCollector collector = getTestCollector();
        MethodMetric methodMetric = getTestMethodMetric();

        List<Double> requestTimes = new ArrayList<>(10000);

        for (int i = 0; i < 300; i++) {
            requestTimes.add(1000 * Math.random());
        }

        Collections.sort(requestTimes);
        double p95Index = 0.95 * (requestTimes.size() - 1);
        double p99Index = 0.99 * (requestTimes.size() - 1);

        double manualP95 = requestTimes.get((int) Math.round(p95Index));
        double manualP99 = requestTimes.get((int) Math.round(p99Index));

        for (Double requestTime : requestTimes) {
            collector.onEvent(new RTEvent(applicationModel, methodMetric, requestTime.longValue()));
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

        //An error of less than 5% is allowed
        Assertions.assertTrue(Math.abs(1 - p95 / manualP95) < 0.05);
        Assertions.assertTrue(Math.abs(1 - p99 / manualP99) < 0.05);
    }

}

