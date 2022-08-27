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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.MetricsConstants.*;

public class AggregateMetricsCollectorTest {

    private ApplicationModel applicationModel;
    private DefaultMetricsCollector defaultCollector;

    private String interfaceName;
    private String methodName;
    private String group;
    private String version;

    @BeforeEach
    public void setup() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);

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
        group = "mockGroup";
        version = "1.0.0";
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    public void testRequestsMetrics() {
        AggregateMetricsCollector collector = new AggregateMetricsCollector(applicationModel);
        defaultCollector.increaseTotalRequests(interfaceName, methodName, group, version);
        defaultCollector.increaseSucceedRequests(interfaceName, methodName, group, version);
        defaultCollector.increaseFailedRequests(interfaceName, methodName, group, version);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
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
        defaultCollector.addRT(interfaceName, methodName, group, version, 10L);

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

        Assertions.assertTrue(sampleMap.containsKey("rt.p99"));
        Assertions.assertTrue(sampleMap.containsKey("rt.p95"));
    }
}
