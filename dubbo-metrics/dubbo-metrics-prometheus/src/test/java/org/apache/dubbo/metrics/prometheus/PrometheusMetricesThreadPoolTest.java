package org.apache.dubbo.metrics.prometheus;
import com.sun.net.httpserver.HttpServer;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.nested.PrometheusConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.collector.sample.ThreadRejectMetricsCountSampler;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.MetricsConstants.*;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;


public class PrometheusMetricesThreadPoolTest {

    private FrameworkModel   frameworkModel;

    private ApplicationModel applicationModel;

    private MetricsConfig    metricsConfig;

    DefaultMetricsCollector metricsCollector;

    @BeforeEach
    public void setup() {
        applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel.getApplicationConfigManager().setApplication(config);
        metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol(PROTOCOL_PROMETHEUS);
        frameworkModel = FrameworkModel.defaultModel();
        metricsCollector = frameworkModel.getBeanFactory().getOrRegisterBean(DefaultMetricsCollector.class);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testExporterThreadpoolName() {
        int port = 30899;
        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        exporter.setMetricsPort(port);
        exporter.setEnabled(true);
        exporter.setMetricsPath("/metrics");

        prometheusConfig.setExporter(exporter);
        metricsConfig.setPrometheus(prometheusConfig);
        metricsConfig.setEnableJvmMetrics(false);
        metricsConfig.setEnableThreadpoolMetrics(true);
        metricsCollector.setCollectEnabled(true);
        metricsCollector.collectApplication(applicationModel);
        PrometheusMetricsReporter reporter = new PrometheusMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();
        exportHttpServer(reporter,port);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(metricsConfig.getEnableThreadpoolMetrics()) {
            metricsCollector.registryDefaultSample();
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:" + port + "/metrics");
            CloseableHttpResponse response = client.execute(request);
            InputStream inputStream = response.getEntity().getContent();
            String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
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
            HttpServer prometheusExporterHttpServer = HttpServer.create(new InetSocketAddress(port), 0);
            prometheusExporterHttpServer.createContext("/metrics", httpExchange -> {
                reporter.refreshData();
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
        DefaultMetricsCollector collector = new DefaultMetricsCollector();
        collector.setCollectEnabled(true);
        collector.setApplicationName(applicationModel.getApplicationName());
        String threadPoolExecutorName="DubboServerHandler-20816";
        ThreadRejectMetricsCountSampler threadRejectMetricsCountSampler=new ThreadRejectMetricsCountSampler(collector);
        threadRejectMetricsCountSampler.incOnEvent(threadPoolExecutorName,threadPoolExecutorName);
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
