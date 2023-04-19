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
package org.apache.dubbo.metrics.sampler;

import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.collector.sample.MethodMetricsSampler;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.observation.MockInvocation;
import org.apache.dubbo.rpc.Invocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MethodMetricsTest {

    private DefaultMetricsCollector collector;
    private MethodMetricsSampler sampler;

    private Invocation invocation;

    @BeforeEach
    public void setUp() {
        collector = new DefaultMetricsCollector();
        sampler = new MethodMetricsSampler(collector);
        invocation = spy(new MockInvocation());
        when(invocation.getTargetServiceUniqueName()).thenReturn("TestService-1");
    }

    @Test
    void testRequestsCount() {
        final long requestTimes = 1000L;

        //METRIC_REQUESTS
        for (long i = 0; i < requestTimes; i++) {
            sampler.inc(invocation, MetricsEvent.Type.TOTAL.getNameByType("provider"));
        }

        List<MetricSample> samples = sampler.sample();

        MetricSample requestsSample = samples.stream()
            .filter(sample -> MetricsKey.METRIC_REQUESTS.getNameByType("provider").equals(sample.getName()))
            .findFirst()
            .orElse(null);

        Assertions.assertNotNull(requestsSample, "METRIC_REQUESTS sample should not be null");
        Assertions.assertEquals(MetricSample.Type.COUNTER, requestsSample.getType(), "METRIC_REQUESTS sample should have a COUNTER type");
        Assertions.assertTrue(requestsSample instanceof CounterMetricSample);
        Assertions.assertEquals(requestTimes, ((CounterMetricSample) requestsSample).getValue().longValue());
    }

    @Test
    void testRequestsProcessing() {
        final long requestTimes = 1000L;

        //METRIC_REQUESTS
        for (long i = 0; i < requestTimes; i++) {
            sampler.inc(invocation, MetricsEvent.Type.PROCESSING.getNameByType("provider"));
        }

        List<MetricSample> samples = sampler.sample();

        MetricSample requestsSample = samples.stream()
            .filter(sample -> MetricsKey.METRIC_REQUESTS_PROCESSING.getNameByType("provider").equals(sample.getName()))
            .findFirst()
            .orElse(null);

        Assertions.assertNotNull(requestsSample, "METRIC_REQUESTS_PROCESSING sample should not be null");
        Assertions.assertEquals(MetricSample.Type.GAUGE, requestsSample.getType(), "METRIC_REQUESTS_PROCESSING sample should have a GAUGE type");
        Assertions.assertTrue(requestsSample instanceof GaugeMetricSample);
        Assertions.assertEquals(requestTimes, ((AtomicLong) ((GaugeMetricSample) requestsSample).getValue()).get());

        for (long i = 0; i < requestTimes; i++) {
            sampler.dec(invocation, MetricsEvent.Type.PROCESSING.getNameByType("provider"));
        }

        samples = sampler.sample();

        requestsSample = samples.stream()
            .filter(sample -> MetricsKey.METRIC_REQUESTS_PROCESSING.getNameByType("provider").equals(sample.getName()))
            .findFirst()
            .orElse(null);

        Assertions.assertEquals(0, ((AtomicLong) ((GaugeMetricSample) requestsSample).getValue()).get());
    }

    @Test
    void testRequestSucceed() {
        final long requestTimes = 1000L;

        //METRIC_REQUESTS_SUCCEED
        for (long i = 0; i < requestTimes; i++) {
            sampler.inc(invocation, MetricsEvent.Type.SUCCEED.getNameByType("provider"));
        }

        List<MetricSample> samples = sampler.sample();

        MetricSample requestsSample = samples.stream()
            .filter(sample -> MetricsKey.METRIC_REQUESTS_SUCCEED.getNameByType("provider").equals(sample.getName()))
            .findFirst()
            .orElse(null);

        Assertions.assertNotNull(requestsSample, "METRIC_REQUESTS_SUCCEED sample should not be null");
        Assertions.assertEquals(MetricSample.Type.COUNTER, requestsSample.getType(), "METRIC_REQUESTS_SUCCEED sample should have a COUNTER type");
        Assertions.assertTrue(requestsSample instanceof CounterMetricSample);
        Assertions.assertEquals(requestTimes, ((AtomicLong) ((CounterMetricSample) requestsSample).getValue()).get());
    }

}
