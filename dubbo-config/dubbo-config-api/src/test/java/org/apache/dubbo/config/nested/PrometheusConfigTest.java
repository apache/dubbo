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
package org.apache.dubbo.config.nested;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class PrometheusConfigTest {

    @Test
    void testExporter() {
        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();

        exporter.setEnabled(true);
        exporter.setEnableHttpServiceDiscovery(true);
        exporter.setHttpServiceDiscoveryUrl("localhost:8080");
        exporter.setMetricsPath("/metrics");
        exporter.setMetricsPort(20888);
        prometheusConfig.setExporter(exporter);

        assertThat(prometheusConfig.getExporter().getEnabled(), equalTo(true));
        assertThat(prometheusConfig.getExporter().getEnableHttpServiceDiscovery(), equalTo(true));
        assertThat(prometheusConfig.getExporter().getHttpServiceDiscoveryUrl(), equalTo("localhost:8080"));
        assertThat(prometheusConfig.getExporter().getMetricsPort(), equalTo(20888));
        assertThat(prometheusConfig.getExporter().getMetricsPath(), equalTo("/metrics"));
    }

    @Test
    void testPushgateway() {
        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Pushgateway pushgateway = new PrometheusConfig.Pushgateway();

        pushgateway.setEnabled(true);
        pushgateway.setBaseUrl("localhost:9091");
        pushgateway.setUsername("username");
        pushgateway.setPassword("password");
        pushgateway.setJob("job");
        pushgateway.setPushInterval(30);
        prometheusConfig.setPushgateway(pushgateway);

        assertThat(prometheusConfig.getPushgateway().getEnabled(), equalTo(true));
        assertThat(prometheusConfig.getPushgateway().getBaseUrl(), equalTo("localhost:9091"));
        assertThat(prometheusConfig.getPushgateway().getUsername(), equalTo("username"));
        assertThat(prometheusConfig.getPushgateway().getPassword(), equalTo("password"));
        assertThat(prometheusConfig.getPushgateway().getJob(), equalTo("job"));
        assertThat(prometheusConfig.getPushgateway().getPushInterval(), equalTo(30));
    }
}