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
import org.apache.dubbo.metrics.collector.sample.ThreadRejectMetricsCountSampler;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_THREAD_NAME;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

public class PrometheusMetricsThreadPoolTest {

    private ApplicationModel applicationModel;

    private MetricsConfig metricsConfig;

    DefaultMetricsCollector metricsCollector;

    HttpServer prometheusExporterHttpServer;

    @BeforeEach
    public void setup() {
        applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel.getApplicationConfigManager().setApplication(config);
        metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol(PROTOCOL_PROMETHEUS);
        metricsCollector = applicationModel.getBeanFactory().getOrRegisterBean(DefaultMetricsCollector.class);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
        if (prometheusExporterHttpServer != null) {
            prometheusExporterHttpServer.stop(0);
        }
    }

    @Test
    void testExporterThreadpoolName() {
        int port = 30899;
        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        exporter.setEnabled(true);

        prometheusConfig.setExporter(exporter);
        metricsConfig.setPrometheus(prometheusConfig);
        metricsConfig.setEnableJvm(false);
        metricsCollector.setCollectEnabled(true);
        metricsConfig.setEnableThreadpool(true);
        metricsCollector.collectApplication();
        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();
        exportHttpServer(reporter, port);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (metricsConfig.getEnableThreadpool()) {
            metricsCollector.registryDefaultSample();
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:" + port + "/metrics");
            CloseableHttpResponse response = client.execute(request);
            InputStream inputStream = response.getEntity().getContent();
            String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            Assertions.assertTrue(text.contains("dubbo_thread_pool_core_size"));
            Assertions.assertTrue(text.contains("dubbo_thread_pool_thread_count"));
        } catch (Exception e) {
            Assertions.fail(e);
        } finally {
            reporter.destroy();
        }
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

    @Test
    @SuppressWarnings("rawtypes")
    void testThreadPoolRejectMetrics() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);
        collector.setApplicationName(applicationModel.getApplicationName());
        String threadPoolExecutorName = "DubboServerHandler-20816";
        ThreadRejectMetricsCountSampler threadRejectMetricsCountSampler =
                new ThreadRejectMetricsCountSampler(collector);
        threadRejectMetricsCountSampler.inc(threadPoolExecutorName, threadPoolExecutorName);
        threadRejectMetricsCountSampler.addMetricName(threadPoolExecutorName);
        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Assertions.assertTrue(sample instanceof GaugeMetricSample);
            GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
            Map<String, String> tags = gaugeSample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), "MockMetrics");
            Assertions.assertEquals(tags.get(TAG_THREAD_NAME), threadPoolExecutorName);
            Assertions.assertEquals(tags.get(TAG_IP), getLocalHost());
            Assertions.assertEquals(tags.get(TAG_HOSTNAME), getLocalHostName());
            Assertions.assertEquals(gaugeSample.applyAsLong(), 1);
        }
    }
}
