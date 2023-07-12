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

import org.apache.dubbo.config.nested.PrometheusConfig;
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
        prometheusConfig.setExporter(exporter);

        MatcherAssert.assertThat(prometheusConfig.getExporter().getEnabled(), Matchers.equalTo(true));
        MatcherAssert.assertThat(prometheusConfig.getExporter().getEnableHttpServiceDiscovery(), Matchers.equalTo(true));
        MatcherAssert.assertThat(prometheusConfig.getExporter().getHttpServiceDiscoveryUrl(), Matchers.equalTo("localhost:8080"));
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

        MatcherAssert.assertThat(prometheusConfig.getPushgateway().getEnabled(), Matchers.equalTo(true));
        MatcherAssert.assertThat(prometheusConfig.getPushgateway().getBaseUrl(), Matchers.equalTo("localhost:9091"));
        MatcherAssert.assertThat(prometheusConfig.getPushgateway().getUsername(), Matchers.equalTo("username"));
        MatcherAssert.assertThat(prometheusConfig.getPushgateway().getPassword(), Matchers.equalTo("password"));
        MatcherAssert.assertThat(prometheusConfig.getPushgateway().getJob(), Matchers.equalTo("job"));
        MatcherAssert.assertThat(prometheusConfig.getPushgateway().getPushInterval(), Matchers.equalTo(30));
    }
}
