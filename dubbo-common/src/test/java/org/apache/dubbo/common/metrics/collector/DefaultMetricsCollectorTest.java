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

package org.apache.dubbo.common.metrics.collector;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.dubbo.common.metrics.event.MetricsEvent;
import org.apache.dubbo.common.metrics.event.RTEvent;
import org.apache.dubbo.common.metrics.event.RequestEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MetricsKey;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testRequestsMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector(frameworkModel);
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();
        collector.increaseTotalRequests(applicationName, interfaceName, methodName, group, version);
        collector.increaseProcessingRequests(applicationName, interfaceName, methodName, group, version);
        collector.increaseSucceedRequests(applicationName, interfaceName, methodName, group, version);
        collector.increaseUnknownFailedRequests(applicationName, interfaceName, methodName, group, version);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
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

        collector.decreaseProcessingRequests(applicationName, interfaceName, methodName, group, version);
        samples = collector.collect();
        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
            Number number = ((GaugeMetricSample) k).getSupplier().get();
            return number.longValue();
        }));

        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_REQUESTS_PROCESSING.getName()), 0L);
    }

    @Test
    void testRTMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector(frameworkModel);
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();
        collector.addRT(applicationName, interfaceName, methodName, group, version, 10L);
        collector.addRT(applicationName, interfaceName, methodName, group, version, 0L);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
        }

        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
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
        DefaultMetricsCollector collector = new DefaultMetricsCollector(frameworkModel);
        collector.setCollectEnabled(true);

        MockListener mockListener = new MockListener();
        collector.addListener(mockListener);
        String applicationName = applicationModel.getApplicationName();

        collector.increaseTotalRequests(applicationName, interfaceName, methodName, group, version);
        Assertions.assertNotNull(mockListener.getCurEvent());
        Assertions.assertTrue(mockListener.getCurEvent() instanceof RequestEvent);
        Assertions.assertEquals(((RequestEvent) mockListener.getCurEvent()).getType(), RequestEvent.Type.TOTAL);

        collector.addRT(applicationName, interfaceName, methodName, group, version, 5L);
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
