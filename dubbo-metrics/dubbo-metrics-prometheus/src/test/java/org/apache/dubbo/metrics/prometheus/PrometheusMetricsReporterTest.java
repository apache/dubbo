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
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;
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

        String response = reporter.getResponse();
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.isEmpty());
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

    @Test
    void testPushgatewayUsesBasicAuthWithNewAdapter() throws Exception {
        CountDownLatch requestReceived = new CountDownLatch(1);
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        String username = "demo";
        String password = "secret";
        String job = "auth-job";

        prometheusExporterHttpServer = HttpServer.create(new InetSocketAddress(0), 0);
        prometheusExporterHttpServer.createContext("/", httpExchange -> {
            requestMethod.set(httpExchange.getRequestMethod());
            requestPath.set(httpExchange.getRequestURI().getPath());
            authorization.set(httpExchange.getRequestHeaders().getFirst("Authorization"));
            try (InputStream inputStream = httpExchange.getRequestBody()) {
                requestBody.set(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n")));
            }
            httpExchange.sendResponseHeaders(202, -1);
            httpExchange.close();
            requestReceived.countDown();
        });
        prometheusExporterHttpServer.start();

        PrometheusMetricsReporter reporter = createPushgatewayReporter(
                "localhost:" + prometheusExporterHttpServer.getAddress().getPort(), job, username, password);
        try {
            Assertions.assertEquals(
                    "io.micrometer.prometheusmetrics.PrometheusMeterRegistry",
                    reporter.getPrometheusRegistry().getClass().getName());

            Assertions.assertTrue(requestReceived.await(10, TimeUnit.SECONDS));
            Assertions.assertEquals("POST", requestMethod.get());
            Assertions.assertEquals("/metrics/job/" + job, requestPath.get());
            Assertions.assertEquals(basicAuthHeader(username, password), authorization.get());
            Assertions.assertTrue(requestBody.get().contains("jvm_memory_used_bytes"));
        } finally {
            reporter.destroy();
        }
    }

    private void exportHttpServer(PrometheusMetricsReporter reporter, int port) {

        try {
            prometheusExporterHttpServer = HttpServer.create(new InetSocketAddress(port), 0);
            prometheusExporterHttpServer.createContext("/metrics", httpExchange -> {
                reporter.resetIfSamplesChanged();
                String response = reporter.getResponse();
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

    private PrometheusMetricsReporter createPushgatewayReporter(
            String baseUrl, String job, String username, String password) {
        metricsConfig.setEnableJvm(true);
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("metrics-test"));

        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Pushgateway pushgateway = new PrometheusConfig.Pushgateway();
        pushgateway.setEnabled(true);
        pushgateway.setBaseUrl(baseUrl);
        pushgateway.setJob(job);
        pushgateway.setPushInterval(1);
        pushgateway.setUsername(username);
        pushgateway.setPassword(password);
        prometheusConfig.setPushgateway(pushgateway);
        metricsConfig.setPrometheus(prometheusConfig);

        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();
        return reporter;
    }

    private String basicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
