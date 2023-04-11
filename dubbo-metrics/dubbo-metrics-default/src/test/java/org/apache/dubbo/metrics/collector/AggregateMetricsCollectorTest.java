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
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.TestMetricsInvoker;
import org.apache.dubbo.metrics.collector.sample.MethodMetricsSampler;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;

class AggregateMetricsCollectorTest {

    private ApplicationModel applicationModel;
    private DefaultMetricsCollector defaultCollector;

    private String interfaceName;
    private String methodName;
    private String group;
    private String version;
    private RpcInvocation invocation;
    private String side;

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
}
