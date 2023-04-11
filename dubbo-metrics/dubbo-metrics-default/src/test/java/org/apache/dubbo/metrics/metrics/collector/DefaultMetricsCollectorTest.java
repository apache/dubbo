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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.TestMetricsInvoker;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.collector.sample.MethodMetricsSampler;
import org.apache.dubbo.metrics.event.MethodEvent;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

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

class DefaultMetricsCollectorTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private String interfaceName;
    private String methodName;
    private String group;
    private String version;
    private RpcInvocation invocation;
    private String side;

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
        side = CommonConstants.CONSUMER;

        invocation.setInvoker(new TestMetricsInvoker(side));
        RpcContext.getServiceContext().setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=" + side));

    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testRequestsMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector();
        collector.setCollectEnabled(true);
        collector.setApplicationName(applicationModel.getApplicationName());

        MethodMetricsSampler methodMetricsCountSampler = collector.getMethodSampler();

        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.TOTAL.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.PROCESSING.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.SUCCEED.getNameByType(side));
        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.UNKNOWN_FAILED.getNameByType(side));

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            if(sample instanceof GaugeMetricSample) {
                GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
                Assertions.assertEquals(gaugeSample.applyAsLong(), 1);
            }else if(sample instanceof CounterMetricSample){
                CounterMetricSample counterMetricSample = (CounterMetricSample) sample;
                Assertions.assertEquals(counterMetricSample.getValue().longValue(), 1);
            }

            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);

        }

        methodMetricsCountSampler.dec(invocation, MetricsEvent.Type.PROCESSING.getNameByType(side));
        samples = collector.collect();

        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
            if(k instanceof GaugeMetricSample){
                return ((GaugeMetricSample) k).applyAsLong();
            }else if(k instanceof CounterMetricSample){
                return ((CounterMetricSample)k).getValue().longValue();
            }else{
                throw new RuntimeException("un support sample type");
            }
        }));

        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_REQUESTS_PROCESSING.getNameByType(side)), 0L);
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
            Map<String, String> tags = sample.getTags();

            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_RT_LAST.getNameByType(side)), 0L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_RT_MIN.getNameByType(side)), 0L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_RT_MAX.getNameByType(side)), 10L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_RT_AVG.getNameByType(side)), 5L);
        Assertions.assertEquals(sampleMap.get(MetricsKey.METRIC_RT_SUM.getNameByType(side)), 10L);
    }

    @Test
    void testListener() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector();
        MethodMetricsSampler methodMetricsCountSampler = collector.getMethodSampler();
        collector.setCollectEnabled(true);

        MockListener mockListener = new MockListener();
        collector.addListener(mockListener);
        collector.setApplicationName(applicationModel.getApplicationName());

        methodMetricsCountSampler.incOnEvent(invocation, MetricsEvent.Type.TOTAL.getNameByType(side));
        Assertions.assertNotNull(mockListener.getCurEvent());
        Assertions.assertTrue(mockListener.getCurEvent() instanceof MethodEvent);
        Assertions.assertEquals(((MethodEvent) mockListener.getCurEvent()).getType(),
            MetricsEvent.Type.TOTAL.getNameByType(side));

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
