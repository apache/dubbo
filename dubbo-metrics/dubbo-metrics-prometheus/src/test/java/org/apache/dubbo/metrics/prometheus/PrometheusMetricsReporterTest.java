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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

class PrometheusMetricsReporterTest {

    private MetricsConfig metricsConfig;
    private ApplicationModel applicationModel;
    private FrameworkModel frameworkModel;
    HttpServer prometheusExporterHttpServer;

    @BeforeEach
    public void setup() {
        metricsConfig = new MetricsConfig();
        applicationModel = ApplicationModel.defaultModel();
        metricsConfig.setProtocol(PROTOCOL_PROMETHEUS);
        frameworkModel = FrameworkModel.defaultModel();
        frameworkModel.getBeanFactory().getOrRegisterBean(DefaultMetricsCollector.class);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
        if (prometheusExporterHttpServer != null) {
            prometheusExporterHttpServer.stop(0);
        }
    }

    @Test
    void testJvmMetrics() {
        metricsConfig.setEnableJvm(true);
        String name = "metrics-test";
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(new ApplicationConfig(name));

        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();

        PrometheusMeterRegistry prometheusRegistry = reporter.getPrometheusRegistry();
        Double d1 = prometheusRegistry.getPrometheusRegistry().getSampleValue("none_exist_metric");
        Double d2 = prometheusRegistry
                .getPrometheusRegistry()
                .getSampleValue(
                        "jvm_gc_memory_promoted_bytes_total", new String[] {"application_name"}, new String[] {name});
        Assertions.assertNull(d1);
        Assertions.assertNull(d2);
    }

    @Test
    void testExporter() {
        int port = 31539;
        //            NetUtils.getAvailablePort();
        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        exporter.setEnabled(true);
        prometheusConfig.setExporter(exporter);
        metricsConfig.setPrometheus(prometheusConfig);
        metricsConfig.setEnableJvm(true);

        ApplicationModel.defaultModel()
                .getApplicationConfigManager()
                .setApplication(new ApplicationConfig("metrics-test"));
        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();
        exportHttpServer(reporter, port);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:" + port + "/metrics");
            CloseableHttpResponse response = client.execute(request);
            InputStream inputStream = response.getEntity().getContent();
            String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            Assertions.assertTrue(text.contains("jvm_gc_memory_promoted_bytes_total"));
        } catch (Exception e) {
            Assertions.fail(e);
        } finally {
            reporter.destroy();
        }
    }

    @Test
    void testPushgateway() {
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

    private void exportHttpServer(PrometheusMetricsReporter reporter, int port) {

        try {
            prometheusExporterHttpServer = HttpServer.create(new InetSocketAddress(port), 0);
            prometheusExporterHttpServer.createContext("/metrics", httpExchange -> {
                reporter.resetIfSamplesChanged();
                String response = reporter.getPrometheusRegistry().scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            Thread httpServerThread = new Thread(prometheusExporterHttpServer::start);
            httpServerThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
