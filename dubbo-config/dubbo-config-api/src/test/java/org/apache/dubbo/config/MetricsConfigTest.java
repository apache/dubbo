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
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;
import org.apache.dubbo.config.nested.HistogramConfig;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class MetricsConfigTest {

    @Test
    void testToUrl() {
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

        HistogramConfig histogram = new HistogramConfig();
        histogram.setEnabled(true);
        metrics.setHistogram(histogram);

        URL url = metrics.toUrl();

        MatcherAssert.assertThat(url.getProtocol(), Matchers.equalTo(PROTOCOL_PROMETHEUS));
        MatcherAssert.assertThat(url.getAddress(), Matchers.equalTo("localhost:9090"));
        MatcherAssert.assertThat(url.getHost(), Matchers.equalTo("localhost"));
        MatcherAssert.assertThat(url.getPort(), Matchers.equalTo(9090));
        MatcherAssert.assertThat(url.getParameter("prometheus.exporter.enabled"), Matchers.equalTo("true"));
        MatcherAssert.assertThat(url.getParameter("prometheus.pushgateway.enabled"), Matchers.equalTo("true"));
        MatcherAssert.assertThat(url.getParameter("aggregation.enabled"), Matchers.equalTo("true"));
        MatcherAssert.assertThat(url.getParameter("histogram.enabled"), Matchers.equalTo("true"));
    }

    @Test
    void testProtocol() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setProtocol(PROTOCOL_PROMETHEUS);
        MatcherAssert.assertThat(metrics.getProtocol(), Matchers.equalTo(PROTOCOL_PROMETHEUS));
    }

    @Test
    void testPrometheus() {
        MetricsConfig metrics = new MetricsConfig();

        PrometheusConfig prometheus = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        PrometheusConfig.Pushgateway pushgateway = new PrometheusConfig.Pushgateway();

        exporter.setEnabled(true);
        exporter.setEnableHttpServiceDiscovery(true);
        exporter.setHttpServiceDiscoveryUrl("localhost:8080");
        prometheus.setExporter(exporter);

        pushgateway.setEnabled(true);
        pushgateway.setBaseUrl("localhost:9091");
        pushgateway.setUsername("username");
        pushgateway.setPassword("password");
        pushgateway.setJob("job");
        pushgateway.setPushInterval(30);
        prometheus.setPushgateway(pushgateway);

        metrics.setPrometheus(prometheus);

        MatcherAssert.assertThat(metrics.getPrometheus().getExporter().getEnabled(), Matchers.equalTo(true));
        MatcherAssert.assertThat(metrics.getPrometheus().getExporter().getEnableHttpServiceDiscovery(), Matchers.equalTo(true));
        MatcherAssert.assertThat(metrics.getPrometheus().getExporter().getHttpServiceDiscoveryUrl(), Matchers.equalTo("localhost:8080"));
        MatcherAssert.assertThat(metrics.getPrometheus().getPushgateway().getEnabled(), Matchers.equalTo(true));
        MatcherAssert.assertThat(metrics.getPrometheus().getPushgateway().getBaseUrl(), Matchers.equalTo("localhost:9091"));
        MatcherAssert.assertThat(metrics.getPrometheus().getPushgateway().getUsername(), Matchers.equalTo("username"));
        MatcherAssert.assertThat(metrics.getPrometheus().getPushgateway().getPassword(), Matchers.equalTo("password"));
        MatcherAssert.assertThat(metrics.getPrometheus().getPushgateway().getJob(), Matchers.equalTo("job"));
        MatcherAssert.assertThat(metrics.getPrometheus().getPushgateway().getPushInterval(), Matchers.equalTo(30));
    }

    @Test
    void testAggregation() {
        MetricsConfig metrics = new MetricsConfig();

        AggregationConfig aggregation = new AggregationConfig();
        aggregation.setEnabled(true);
        aggregation.setBucketNum(5);
        aggregation.setTimeWindowSeconds(120);
        metrics.setAggregation(aggregation);

        MatcherAssert.assertThat(metrics.getAggregation().getEnabled(), Matchers.equalTo(true));
        MatcherAssert.assertThat(metrics.getAggregation().getBucketNum(), Matchers.equalTo(5));
        MatcherAssert.assertThat(metrics.getAggregation().getTimeWindowSeconds(), Matchers.equalTo(120));
    }
}
