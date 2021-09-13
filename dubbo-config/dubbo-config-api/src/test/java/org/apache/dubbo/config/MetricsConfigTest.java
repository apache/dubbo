/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class MetricsConfigTest {

    @Test
    public void testProtocol() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setProtocol("prometheus");
        assertThat(metrics.getProtocol(), equalTo("prometheus"));
    }

    @Test
    public void testMode() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setMode("push");
        assertThat(metrics.getMode(), equalTo("push"));
    }

    @Test
    public void testAddress() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setAddress("localhost:9091");
        assertThat(metrics.getAddress(), equalTo("localhost:9091"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, metrics);
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testMetricsPort() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setMetricsPort(20888);
        assertThat(metrics.getMetricsPort(), equalTo(20888));
    }

    @Test
    public void testMetricsPath() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setMetricsPath("/metrics");
        assertThat(metrics.getMetricsPath(), equalTo("/metrics"));
    }

    @Test
    public void testAggregation() {
        MetricsConfig metrics = new MetricsConfig();

        MetricsConfig.Aggregation aggregation = new MetricsConfig.Aggregation();
        aggregation.setEnable(true);
        aggregation.setBucketNum(5);
        aggregation.setTimeWindowSeconds(120);
        metrics.setAggregation(aggregation);

        assertThat(metrics.getAggregation().getEnable(), equalTo(true));
        assertThat(metrics.getAggregation().getBucketNum(), equalTo(5));
        assertThat(metrics.getAggregation().getTimeWindowSeconds(), equalTo(120));
    }
}
