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

import org.apache.dubbo.metrics.collector.sample.MetricsCountSampleConfigurer;
import org.apache.dubbo.metrics.collector.sample.SimpleMetricsCountSampler;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

public class CountSamplerTest {

    public RequestMetricsCountSampler sampler = new RequestMetricsCountSampler();

    @BeforeEach
    public void before() {
        sampler = new RequestMetricsCountSampler();
    }

    @Test
    public void rtTest() {
        String applicationName = "test";

        sampler.addRT(applicationName, RT.METHOD_REQUEST, 2L);
        RequestMethodMetrics metrics = new RequestMethodMetrics(applicationName);

        ConcurrentMap<RequestMethodMetrics, AtomicLong> avgRT = sampler.getAvgRT(RT.METHOD_REQUEST);
        Assertions.assertNotNull(avgRT.get(metrics).intValue()==2);

        ConcurrentMap<RequestMethodMetrics, LongAccumulator> maxRT = sampler.getMaxRT(RT.METHOD_REQUEST);
        Assertions.assertNotNull(maxRT.get(metrics).equals(2));

        ConcurrentMap<RequestMethodMetrics, AtomicLong> totalRT = sampler.getTotalRT(RT.METHOD_REQUEST);
        Assertions.assertNotNull(totalRT.get(metrics).equals(1));

        ConcurrentMap<RequestMethodMetrics, AtomicLong> rtCount = sampler.getRtCount(RT.METHOD_REQUEST);
        Assertions.assertTrue(rtCount.get(metrics).intValue()==1);

        ConcurrentMap<RequestMethodMetrics, AtomicLong> lastRT = sampler.getLastRT(RT.METHOD_REQUEST);
        Assertions.assertTrue(lastRT.get(metrics).intValue()==2);

        ConcurrentMap<RequestMethodMetrics, LongAccumulator> minRT = sampler.getMinRT(RT.METHOD_REQUEST);
        Assertions.assertTrue(minRT.get(metrics).intValue()==2);


        sampler.addRT(applicationName, RT.METHOD_REQUEST, 4L);

        ConcurrentMap<RequestMethodMetrics, AtomicLong> avgRT2 = sampler.getAvgRT(RT.METHOD_REQUEST);
        Assertions.assertNotNull(avgRT2.get(metrics).intValue()==3);

        ConcurrentMap<RequestMethodMetrics, LongAccumulator> maxRT2 = sampler.getMaxRT(RT.METHOD_REQUEST);
        Assertions.assertNotNull(maxRT2.get(metrics).equals(4));

        ConcurrentMap<RequestMethodMetrics, AtomicLong> totalRT2 = sampler.getTotalRT(RT.METHOD_REQUEST);
        Assertions.assertNotNull(totalRT2.get(metrics).intValue()==2);

        ConcurrentMap<RequestMethodMetrics, AtomicLong> rtCount2 = sampler.getRtCount(RT.METHOD_REQUEST);
        Assertions.assertTrue(rtCount2.get(metrics).intValue()==2);

        ConcurrentMap<RequestMethodMetrics, AtomicLong> lastRT2 = sampler.getLastRT(RT.METHOD_REQUEST);
        Assertions.assertTrue(lastRT2.get(metrics).intValue()==4);

        ConcurrentMap<RequestMethodMetrics, LongAccumulator> minRT2 = sampler.getMinRT(RT.METHOD_REQUEST);
        Assertions.assertTrue(minRT2.get(metrics).intValue()==2);

    }

    public class RequestMetricsCountSampler
        extends SimpleMetricsCountSampler<String, RT, RequestMethodMetrics> {

        @Override
        public List<MetricSample> sample() {
            return null;
        }

        @Override
        protected void countConfigure(
            MetricsCountSampleConfigurer<String, RT, RequestMethodMetrics> sampleConfigure) {
            sampleConfigure.configureMetrics(
                configure -> new RequestMethodMetrics(configure.getSource()));
            sampleConfigure.configureEventHandler(configure -> {
                System.out.println("发布事件");
            });
        }

        @Override
        public void rtConfigure(
            MetricsCountSampleConfigurer<String, RT, RequestMethodMetrics> sampleConfigure) {
            sampleConfigure.configureMetrics(configure -> new RequestMethodMetrics(configure.getSource()));
            sampleConfigure.configureEventHandler(configure -> {
                System.out.println("rt事件");
            });
        }
    }

    static enum RT{
        METHOD_REQUEST
    }

    static class RequestMethodMetrics implements Metric {

        private String applicationName;

        public RequestMethodMetrics(String applicationName) {
            this.applicationName=applicationName;
        }
        @Override
        public Map<String, String> getTags() {
            Map<String,String> tags = new HashMap<>();
            tags.put("serviceName", "test");
            tags.put("version", "1.0.0");
            tags.put("uptime", "20220202");
            return tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof RequestMethodMetrics))
                return false;
            RequestMethodMetrics that = (RequestMethodMetrics) o;
            return Objects.equals(applicationName, that.applicationName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(applicationName);
        }
    }

}

