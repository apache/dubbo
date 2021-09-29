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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MetricsConfigTest {

    @Test
    public void testToUrl() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setProtocol(PROTOCOL_PROMETHEUS);

        PrometheusConfig prometheus = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        PrometheusConfig.Pushgateway pushgateway = new PrometheusConfig.Pushgateway();

        exporter.setEnabled(true);
        pushgateway.setEnabled(true);
        prometheus.setExporter(exporter);
        prometheus.setPushgateway(pushgateway);
        metrics.setPrometheus(prometheus);

        AggregationConfig aggregation = new AggregationConfig();
        aggregation.setEnabled(true);
        metrics.setAggregation(aggregation);

        URL url = metrics.toUrl();

        assertThat(url.getProtocol(), equalTo(PROTOCOL_PROMETHEUS));
        assertThat(url.getAddress(), equalTo("localhost:9090"));
        assertThat(url.getHost(), equalTo("localhost"));
        assertThat(url.getPort(), equalTo(9090));
        assertThat(url.getParameter("prometheus.exporter.enabled"), equalTo("true"));
        assertThat(url.getParameter("prometheus.pushgateway.enabled"), equalTo("true"));
        assertThat(url.getParameter("aggregation.enabled"), equalTo("true"));
    }

    @Test
    public void testProtocol() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setProtocol(PROTOCOL_PROMETHEUS);
        assertThat(metrics.getProtocol(), equalTo(PROTOCOL_PROMETHEUS));
    }

    @Test
    public void testPrometheus() {
        MetricsConfig metrics = new MetricsConfig();

        PrometheusConfig prometheus = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        PrometheusConfig.Pushgateway pushgateway = new PrometheusConfig.Pushgateway();

        exporter.setEnabled(true);
        exporter.setEnableHttpServiceDiscovery(true);
        exporter.setHttpServiceDiscoveryUrl("localhost:8080");
        exporter.setMetricsPath("/metrics");
        exporter.setMetricsPort(20888);
        prometheus.setExporter(exporter);

        pushgateway.setEnabled(true);
        pushgateway.setBaseUrl("localhost:9091");
        pushgateway.setUsername("username");
        pushgateway.setPassword("password");
        pushgateway.setJob("job");
        pushgateway.setPushInterval(30);
        prometheus.setPushgateway(pushgateway);

        metrics.setPrometheus(prometheus);

        assertThat(metrics.getPrometheus().getExporter().getEnabled(), equalTo(true));
        assertThat(metrics.getPrometheus().getExporter().getEnableHttpServiceDiscovery(), equalTo(true));
        assertThat(metrics.getPrometheus().getExporter().getHttpServiceDiscoveryUrl(), equalTo("localhost:8080"));
        assertThat(metrics.getPrometheus().getExporter().getMetricsPort(), equalTo(20888));
        assertThat(metrics.getPrometheus().getExporter().getMetricsPath(), equalTo("/metrics"));
        assertThat(metrics.getPrometheus().getPushgateway().getEnabled(), equalTo(true));
        assertThat(metrics.getPrometheus().getPushgateway().getBaseUrl(), equalTo("localhost:9091"));
        assertThat(metrics.getPrometheus().getPushgateway().getUsername(), equalTo("username"));
        assertThat(metrics.getPrometheus().getPushgateway().getPassword(), equalTo("password"));
        assertThat(metrics.getPrometheus().getPushgateway().getJob(), equalTo("job"));
        assertThat(metrics.getPrometheus().getPushgateway().getPushInterval(), equalTo(30));
    }

    @Test
    public void testAggregation() {
        MetricsConfig metrics = new MetricsConfig();

        AggregationConfig aggregation = new AggregationConfig();
        aggregation.setEnabled(true);
        aggregation.setBucketNum(5);
        aggregation.setTimeWindowSeconds(120);
        metrics.setAggregation(aggregation);

        assertThat(metrics.getAggregation().getEnabled(), equalTo(true));
        assertThat(metrics.getAggregation().getBucketNum(), equalTo(5));
        assertThat(metrics.getAggregation().getTimeWindowSeconds(), equalTo(120));
    }
}
