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

import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_PARAMETER_TYPES_DESC;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;

public class AggregateMetricsCollectorTest {

    private static ApplicationModel applicationModel;
    private static DefaultMetricsCollector defaultCollector;

    private static String interfaceName;
    private static String methodName;
    private static String parameterTypesDesc;
    private static String group;
    private static String version;

    @BeforeAll
    public static void setup() {
        applicationModel = ApplicationModel.defaultModel();
        defaultCollector = new DefaultMetricsCollector(applicationModel);
        defaultCollector.setCollectEnabled(true);
        MetricsConfig metricsConfig = new MetricsConfig();
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        aggregationConfig.setBucketNum(12);
        aggregationConfig.setTimeWindowSeconds(120);
        metricsConfig.setAggregation(aggregationConfig);
        applicationModel.getApplicationConfigManager().setMetrics(metricsConfig);
        applicationModel.getBeanFactory().registerBean(defaultCollector);

        interfaceName = "org.apache.dubbo.MockInterface";
        methodName = "mockMethod";
        parameterTypesDesc = "Ljava/lang/String;";
        group = "mockGroup";
        version = "1.0.0";
    }

    @Test
    public void testRequestsMetrics() {
        AggregateMetricsCollector collector = new AggregateMetricsCollector(applicationModel);
        defaultCollector.increaseTotalRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        defaultCollector.increaseSucceedRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        defaultCollector.increaseFailedRequests(interfaceName, methodName, parameterTypesDesc, group, version);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_PARAMETER_TYPES_DESC), parameterTypesDesc);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
        }

        samples = collector.collect();
        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
            Number number = ((GaugeMetricSample) k).getSupplier().get();
            return number.longValue();
        }));

        Assertions.assertEquals(sampleMap.get("requests.total.aggregate"), 1L);
        Assertions.assertEquals(sampleMap.get("requests.succeed.aggregate"), 1L);
        Assertions.assertEquals(sampleMap.get("requests.failed.aggregate"), 1L);
        Assertions.assertTrue(sampleMap.containsKey("qps"));
    }

    @Test
    public void testRTMetrics() {
        AggregateMetricsCollector collector = new AggregateMetricsCollector(applicationModel);
        defaultCollector.addRT(interfaceName, methodName, parameterTypesDesc, group, version, 10L);

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

        Assertions.assertTrue(sampleMap.containsKey("rt.p99"));
        Assertions.assertTrue(sampleMap.containsKey("rt.p95"));
    }
}
