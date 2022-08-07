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

package org.apache.dubbo.metrics.prometheus;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

public class PrometheusMetricsReporterTest {

    private MetricsConfig metricsConfig;
    private ApplicationModel applicationModel;

    @BeforeEach
    public void setup() {
        metricsConfig = new MetricsConfig();
        applicationModel = ApplicationModel.defaultModel();
        metricsConfig.setProtocol(PROTOCOL_PROMETHEUS);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    public void testJvmMetrics() {
        metricsConfig.setEnableJvmMetrics(true);
        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();

        PrometheusMeterRegistry prometheusRegistry = reporter.getPrometheusRegistry();
        Double d1 = prometheusRegistry.getPrometheusRegistry().getSampleValue("none_exist_metric");
        Double d2 = prometheusRegistry.getPrometheusRegistry().getSampleValue("jvm_gc_memory_promoted_bytes_total");

        Assertions.assertNull(d1);
        Assertions.assertNotNull(d2);
    }

    @Test
    public void testExporter() {
        int port = NetUtils.getAvailablePort();

        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        exporter.setMetricsPort(port);
        exporter.setMetricsPath("/metrics");
        exporter.setEnabled(true);
        prometheusConfig.setExporter(exporter);
        metricsConfig.setPrometheus(prometheusConfig);
        metricsConfig.setEnableJvmMetrics(true);

        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:" + port + "/metrics");
            CloseableHttpResponse response = client.execute(request);
            InputStream inputStream = response.getEntity().getContent();
            String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            Assertions.assertTrue(text.contains("jvm_gc_memory_promoted_bytes_total"));
        } catch (Exception e) {
            Assertions.fail(e);
        } finally {
            reporter.destroy();
        }
    }

    @Test
    public void testPushgateway() {
        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Pushgateway pushgateway = new PrometheusConfig.Pushgateway();
        pushgateway.setJob("mock");
        pushgateway.setBaseUrl("localhost:9091");
        pushgateway.setEnabled(true);
        pushgateway.setPushInterval(1);
        prometheusConfig.setPushgateway(pushgateway);
        metricsConfig.setPrometheus(prometheusConfig);

        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();

        ScheduledExecutorService executor = reporter.getPushJobExecutor();
        Assertions.assertTrue(executor != null && !executor.isTerminated() && !executor.isShutdown());

        reporter.destroy();
        Assertions.assertTrue(executor.isTerminated() || executor.isShutdown());
    }
}
