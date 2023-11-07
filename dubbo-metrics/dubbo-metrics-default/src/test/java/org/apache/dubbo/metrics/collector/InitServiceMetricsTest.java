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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.event.MetricsInitEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
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

import static org.apache.dubbo.metrics.DefaultConstants.INIT_AGG_METHOD_KEYS;
import static org.apache.dubbo.metrics.DefaultConstants.INIT_DEFAULT_METHOD_KEYS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_PROCESSING;

class InitServiceMetricsTest {

    private ApplicationModel applicationModel;

    private String interfaceName;
    private String methodName;
    private String group;
    private String version;

    private String side;

    private DefaultMetricsCollector defaultCollector;

    private AggregateMetricsCollector aggregateMetricsCollector;

    @BeforeEach
    public void setup() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        MetricsConfig metricsConfig = new MetricsConfig();
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        aggregationConfig.setBucketNum(12);
        aggregationConfig.setTimeWindowSeconds(120);
        metricsConfig.setAggregation(aggregationConfig);

        applicationModel.getApplicationConfigManager().setMetrics(metricsConfig);
        applicationModel.getApplicationConfigManager().setApplication(config);

        defaultCollector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        defaultCollector.setCollectEnabled(true);

        aggregateMetricsCollector =
                applicationModel.getBeanFactory().getOrRegisterBean(AggregateMetricsCollector.class);
        aggregateMetricsCollector.setCollectEnabled(true);

        interfaceName = "org.apache.dubbo.MockInterface";
        methodName = "mockMethod";
        group = "mockGroup";
        version = "1.0.0";
        side = CommonConstants.PROVIDER_SIDE;

        String serviceKey = group + "/" + interfaceName + ":" + version;

        String protocolServiceKey = serviceKey + ":dubbo";

        RpcInvocation invocation = new RpcInvocation(
                serviceKey, null, methodName, interfaceName, protocolServiceKey, null, null, null, null, null, null);
        MetricsEventBus.publish(MetricsInitEvent.toMetricsInitEvent(
                applicationModel, invocation, MethodMetric.isServiceLevel(applicationModel)));
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testMetricsInitEvent() {

        List<MetricSample> metricSamples = defaultCollector.collect();
        // INIT_DEFAULT_METHOD_KEYS.size() = 6
        Assertions.assertEquals(INIT_DEFAULT_METHOD_KEYS.size(), metricSamples.size());
        List<String> metricsNames =
                metricSamples.stream().map(MetricSample::getName).collect(Collectors.toList());

        String REQUESTS =
                new MetricsKeyWrapper(METRIC_REQUESTS, MetricsPlaceValue.of(side, MetricsLevel.SERVICE)).targetKey();
        String PROCESSING = new MetricsKeyWrapper(
                        METRIC_REQUESTS_PROCESSING, MetricsPlaceValue.of(side, MetricsLevel.SERVICE))
                .targetKey();
        Assertions.assertTrue(metricsNames.contains(REQUESTS));
        Assertions.assertTrue(metricsNames.contains(PROCESSING));
        for (MetricSample metricSample : metricSamples) {
            if (metricSample instanceof GaugeMetricSample) {
                GaugeMetricSample<?> gaugeMetricSample = (GaugeMetricSample<?>) metricSample;
                Object objVal = gaugeMetricSample.getValue();
                if (objVal instanceof Map) {
                    Map<ServiceKeyMetric, AtomicLong> value = (Map<ServiceKeyMetric, AtomicLong>) objVal;
                    Assertions.assertTrue(value.values().stream().allMatch(atomicLong -> atomicLong.intValue() == 0));
                }
            } else {
                AtomicLong value = (AtomicLong) ((CounterMetricSample<?>) metricSample).getValue();
                Assertions.assertEquals(0, value.intValue());
            }
        }

        List<MetricSample> samples = aggregateMetricsCollector.collect();
        // INIT_AGG_METHOD_KEYS.size(10) + qps(1) + rt(4) +rtAgr(3)= 18
        Assertions.assertEquals(INIT_AGG_METHOD_KEYS.size() + 1 + 4 + 3, samples.size());

        for (MetricSample metricSample : samples) {
            if (metricSample instanceof GaugeMetricSample) {
                GaugeMetricSample<?> gaugeMetricSample = (GaugeMetricSample<?>) metricSample;
                Object objVal = gaugeMetricSample.getValue();
                if (objVal instanceof TimeWindowCounter) {
                    Assertions.assertEquals(0.0, ((TimeWindowCounter) objVal).get());
                }
            }
        }
    }
}
