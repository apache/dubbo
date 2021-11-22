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

import org.apache.dubbo.common.metrics.event.BaseMetricsEvent;
import org.apache.dubbo.common.metrics.event.NewRTEvent;
import org.apache.dubbo.common.metrics.event.NewRequestEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_PARAMETER_TYPES_DESC;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;

public class DefaultMetricsCollectorTest {

    private static ApplicationModel applicationModel;
    private static String interfaceName;
    private static String methodName;
    private static String parameterTypesDesc;
    private static String group;
    private static String version;

    @BeforeAll
    public static void setup() {
        applicationModel = ApplicationModel.defaultModel();
        interfaceName = "org.apache.dubbo.MockInterface";
        methodName = "mockMethod";
        parameterTypesDesc = "Ljava/lang/String;";
        group = "mockGroup";
        version = "1.0.0";
    }

    @Test
    public void testRequestsMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);
        collector.increaseTotalRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        collector.increaseProcessingRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        collector.increaseSucceedRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        collector.increaseFailedRequests(interfaceName, methodName, parameterTypesDesc, group, version);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Assertions.assertTrue(sample instanceof GaugeMetricSample);
            GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
            Map<String, String> tags = gaugeSample.getTags();
            Supplier<Number> supplier = gaugeSample.getSupplier();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_PARAMETER_TYPES_DESC), parameterTypesDesc);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
            Assertions.assertEquals(supplier.get().longValue(), 1);
        }

        collector.decreaseProcessingRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        samples = collector.collect();
        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
            Number number = ((GaugeMetricSample) k).getSupplier().get();
            return number.longValue();
        }));

        Assertions.assertEquals(sampleMap.get("requests.processing"), 0L);
    }

    @Test
    public void testRTMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);
        collector.addRT(interfaceName, methodName, parameterTypesDesc, group, version, 10L);
        collector.addRT(interfaceName, methodName, parameterTypesDesc, group, version, 0L);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_PARAMETER_TYPES_DESC), parameterTypesDesc);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
        }

        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
            Number number = ((GaugeMetricSample) k).getSupplier().get();
            return number.longValue();
        }));

        Assertions.assertEquals(sampleMap.get("rt.last"), 0L);
        Assertions.assertEquals(sampleMap.get("rt.min"), 0L);
        Assertions.assertEquals(sampleMap.get("rt.max"), 10L);
        Assertions.assertEquals(sampleMap.get("rt.avg"), 5L);
        Assertions.assertEquals(sampleMap.get("rt.total"), 10L);
    }

    @Test
    public void testListener() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);

        MockListener mockListener = new MockListener();
        collector.addListener(mockListener);

        collector.increaseTotalRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        Assertions.assertNotNull(mockListener.getCurEvent());
        Assertions.assertTrue(mockListener.getCurEvent() instanceof NewRequestEvent);
        Assertions.assertEquals(((NewRequestEvent) mockListener.getCurEvent()).getType(), NewRequestEvent.Type.TOTAL);

        collector.addRT(interfaceName, methodName, parameterTypesDesc, group, version, 5L);
        Assertions.assertTrue(mockListener.getCurEvent() instanceof NewRTEvent);
        Assertions.assertEquals(((NewRTEvent) mockListener.getCurEvent()).getRt(), 5L);
    }

    static class MockListener implements MetricsListener {

        private BaseMetricsEvent curEvent;

        @Override
        public void onEvent(BaseMetricsEvent event) {
            curEvent = event;
        }

        public BaseMetricsEvent getCurEvent() {
            return curEvent;
        }
    }
}
