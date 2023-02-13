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

package org.apache.dubbo.metrics.metrics.collector;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.collector.sample.MethodMetricsSampler;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;

class DefaultMetricsCollectorTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private String interfaceName;
    private String methodName;
    private String group;
    private String version;
    private RpcInvocation invocation;
    public static final  String  DUBBO_THREAD_METRIC_MARK = "dubbo.thread.pool";

    @BeforeEach
    public void setup() {
        frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel.getApplicationConfigManager().setApplication(config);

        interfaceName = "org.apache.dubbo.MockInterface";
        methodName = "mockMethod";
        group = "mockGroup";
        version = "1.0.0";

        invocation = new RpcInvocation(methodName, interfaceName, "serviceKey", null, null);
        invocation.setTargetServiceUniqueName(group + "/" + interfaceName + ":" + version);
        invocation.setAttachment(GROUP_KEY, group);
        invocation.setAttachment(VERSION_KEY, version);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testRequestsMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector();
        collector.setCollectEnabled(true);
        collector.setApplicationName(applicationModel.getApplicationName());

        MethodMetricsSampler methodMetricsCountSampler = collector.getMethodSampler();

        methodMetricsCountSampler.incOnEvent(invocation,MetricsEvent.Type.TOTAL);
        methodMetricsCountSampler.incOnEvent(invocation,MetricsEvent.Type.PROCESSING);
        methodMetricsCountSampler.incOnEvent(invocation,MetricsEvent.Type.SUCCEED);
        methodMetricsCountSampler.incOnEvent(invocation,MetricsEvent.Type.UNKNOWN_FAILED);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            if (sample.getName().contains(DUBBO_THREAD_METRIC_MARK)) {
                continue;
            }
            Assertions.assertTrue(sample instanceof GaugeMetricSample);
            GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
            Map<String, String> tags = gaugeSample.getTags();
            Supplier<Number> supplier = gaugeSample.getSupplier();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
            Assertions.assertEquals(supplier.get().longValue(), 1);
        }

        methodMetricsCountSampler.dec(invocation,MetricsEvent.Type.PROCESSING);
        samples = collector.collect();
        List<MetricSample> samples1 = new ArrayList<>();
        for (MetricSample sample : samples) {
            if (sample.getName().contains(DUBBO_THREAD_METRIC_MARK)) {
                continue;
            }
            samples1.add(sample);
        }
        Map<String, Long> sampleMap = samples1.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
            Number number = ((GaugeMetricSample) k).getSupplier().get();
            return number.longValue();
        }));

        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_REQUESTS_PROCESSING.getName()), 0L);
    }

    @Test
    void testRTMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector();
        collector.setCollectEnabled(true);
        MethodMetricsSampler methodMetricsCountSampler = collector.getMethodSampler();
        String applicationName = applicationModel.getApplicationName();

        collector.setApplicationName(applicationName);

        methodMetricsCountSampler.addRT(invocation, 10L);
        methodMetricsCountSampler.addRT(invocation, 0L);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            if (sample.getName().contains(DUBBO_THREAD_METRIC_MARK)) {
                continue;
            }
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
        }
        List<MetricSample> samples1 = new ArrayList<>();
        for (MetricSample sample : samples) {
            if (sample.getName().contains(DUBBO_THREAD_METRIC_MARK)) {
                continue;
            }
            samples1.add(sample);
        }
        Map<String, Long> sampleMap = samples1.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
            Number number = ((GaugeMetricSample) k).getSupplier().get();
            return number.longValue();
        }));

        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_LAST.getName()), 0L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_MIN.getName()), 0L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_MAX.getName()), 10L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_AVG.getName()), 5L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_SUM.getName()), 10L);
    }

    @Test
    void testListener() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector();
        MethodMetricsSampler methodMetricsCountSampler = collector.getMethodSampler();
        collector.setCollectEnabled(true);

        MockListener mockListener = new MockListener();
        collector.addListener(mockListener);
        collector.setApplicationName(applicationModel.getApplicationName());

        methodMetricsCountSampler.incOnEvent(invocation,MetricsEvent.Type.TOTAL);
        Assertions.assertNotNull(mockListener.getCurEvent());
        Assertions.assertTrue(mockListener.getCurEvent() instanceof RequestEvent);
        Assertions.assertEquals(((RequestEvent) mockListener.getCurEvent()).getType(), MetricsEvent.Type.TOTAL);

        methodMetricsCountSampler.addRT(invocation, 5L);
        Assertions.assertTrue(mockListener.getCurEvent() instanceof RTEvent);
        Assertions.assertEquals(((RTEvent) mockListener.getCurEvent()).getRt(), 5L);
    }

    static class MockListener implements MetricsListener {

        private MetricsEvent curEvent;

        @Override
        public void onEvent(MetricsEvent event) {
            curEvent = event;
        }

        public MetricsEvent getCurEvent() {
            return curEvent;
        }
    }
}
