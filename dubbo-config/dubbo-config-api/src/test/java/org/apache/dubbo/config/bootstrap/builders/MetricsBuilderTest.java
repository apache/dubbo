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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.config.nested.HistogramConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetricsBuilderTest {

    @Test
    void protocol() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.protocol("test");
        Assertions.assertEquals("test", builder.build().getProtocol());
    }

    @Test
    void enableJvm() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.enableJvm(true);
        Assertions.assertTrue(builder.build().getEnableJvm());
    }

    @Test
    void enableThreadPool() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.enableThreadPool(false);
        Assertions.assertFalse(builder.build().getEnableThreadpool());
    }

    @Test
    void enableRegistry() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.enableRegistry(true);
        Assertions.assertTrue(builder.build().getEnableRegistry());
    }

    @Test
    void enableMetadata() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.enableMetadata(true);
        Assertions.assertTrue(builder.build().getEnableMetadata());
    }

    @Test
    void exportMetricsService() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.exportMetricsService(false);
        Assertions.assertFalse(builder.build().getExportMetricsService());
    }

    @Test
    void enableMetricsInit() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.enableMetricsInit(true);
        Assertions.assertTrue(builder.build().getEnableMetricsInit());
    }

    @Test
    void enableCollectorSync() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.enableCollectorSync(true);
        Assertions.assertTrue(builder.build().getEnableCollectorSync());
    }

    @Test
    void collectorSyncPeriod() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.collectorSyncPeriod(10);
        Assertions.assertEquals(10, builder.build().getCollectorSyncPeriod());
    }

    @Test
    void exportServiceProtocol() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.exportServiceProtocol("tri");
        Assertions.assertEquals("tri", builder.build().getExportServiceProtocol());
    }

    @Test
    void exportServicePort() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.exportServicePort(2999);
        Assertions.assertEquals(2999, builder.build().getExportServicePort());
    }

    @Test
    void useGlobalRegistry() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.useGlobalRegistry(true);
        Assertions.assertTrue(builder.build().getUseGlobalRegistry());
    }

    @Test
    void enableRpc() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.enableRpc(false);
        Assertions.assertFalse(builder.build().getEnableRpc());
    }

    @Test
    void rpcLevel() {
        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.rpcLevel("SERVICE");
        Assertions.assertEquals("SERVICE", builder.build().getRpcLevel());
    }

    @Test
    void aggregation() {
        AggregationConfig aggregation = new AggregationConfig();
        aggregation.setEnabled(true);
        aggregation.setEnableQps(false);
        aggregation.setBucketNum(100);

        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.aggregation(aggregation);

        Assertions.assertSame(aggregation, builder.build().getAggregation());
        Assertions.assertTrue(builder.build().getAggregation().getEnabled());
        Assertions.assertFalse(builder.build().getAggregation().getEnableQps());
        Assertions.assertEquals(100, builder.build().getAggregation().getBucketNum());
    }

    @Test
    void histogram() {
        HistogramConfig histogram = new HistogramConfig();
        histogram.setEnabled(true);
        histogram.setMaxExpectedMs(1000);
        histogram.setBucketsMs(new Integer[] {100, 200, 300, 400, 500});
        histogram.setPercentiles(new double[] {0.5, 0.9, 0.95, 0.99});

        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.histogram(histogram);

        Assertions.assertSame(histogram, builder.build().getHistogram());
        Assertions.assertTrue(builder.build().getHistogram().getEnabled());
        Assertions.assertEquals(1000, builder.build().getHistogram().getMaxExpectedMs());
        Assertions.assertArrayEquals(
                new Integer[] {100, 200, 300, 400, 500},
                builder.build().getHistogram().getBucketsMs());
        Assertions.assertArrayEquals(
                new double[] {0.5, 0.9, 0.95, 0.99},
                builder.build().getHistogram().getPercentiles());
    }

    @Test
    void prometheus() {
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        exporter.setEnabled(true);
        exporter.setEnableHttpServiceDiscovery(true);
        exporter.setHttpServiceDiscoveryUrl("localhost:8080");

        PrometheusConfig.Pushgateway pushGateway = new PrometheusConfig.Pushgateway();
        pushGateway.setEnabled(true);
        pushGateway.setBaseUrl("localhost:9091");
        pushGateway.setUsername("username");
        pushGateway.setPassword("password");
        pushGateway.setJob("job");
        pushGateway.setPushInterval(30);

        PrometheusConfig prometheus = new PrometheusConfig();
        prometheus.setExporter(exporter);
        prometheus.setPushgateway(pushGateway);

        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.prometheus(prometheus);

        Assertions.assertSame(prometheus, builder.build().getPrometheus());
        Assertions.assertSame(exporter, builder.build().getPrometheus().getExporter());
        Assertions.assertSame(pushGateway, builder.build().getPrometheus().getPushgateway());
        Assertions.assertTrue(builder.build().getPrometheus().getExporter().getEnabled());
        Assertions.assertTrue(builder.build().getPrometheus().getExporter().getEnableHttpServiceDiscovery());
        Assertions.assertEquals(
                "localhost:8080", builder.build().getPrometheus().getExporter().getHttpServiceDiscoveryUrl());
        Assertions.assertTrue(builder.build().getPrometheus().getPushgateway().getEnabled());
        Assertions.assertEquals(
                "localhost:9091",
                builder.build().getPrometheus().getPushgateway().getBaseUrl());
        Assertions.assertEquals(
                "username", builder.build().getPrometheus().getPushgateway().getUsername());
        Assertions.assertEquals(
                "password", builder.build().getPrometheus().getPushgateway().getPassword());
        Assertions.assertEquals(
                "job", builder.build().getPrometheus().getPushgateway().getJob());
        Assertions.assertEquals(
                30, builder.build().getPrometheus().getPushgateway().getPushInterval());
    }

    @Test
    void build() {
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        exporter.setEnabled(true);
        exporter.setEnableHttpServiceDiscovery(true);
        exporter.setHttpServiceDiscoveryUrl("localhost:8080");

        PrometheusConfig.Pushgateway pushGateway = new PrometheusConfig.Pushgateway();
        pushGateway.setEnabled(true);
        pushGateway.setBaseUrl("localhost:9091");
        pushGateway.setUsername("username");
        pushGateway.setPassword("password");
        pushGateway.setJob("job");
        pushGateway.setPushInterval(30);

        PrometheusConfig prometheus = new PrometheusConfig();
        prometheus.setExporter(exporter);
        prometheus.setPushgateway(pushGateway);

        HistogramConfig histogram = new HistogramConfig();
        histogram.setEnabled(true);
        histogram.setMaxExpectedMs(1000);
        histogram.setBucketsMs(new Integer[] {100, 200, 300, 400, 500});
        histogram.setPercentiles(new double[] {0.5, 0.9, 0.95, 0.99});

        AggregationConfig aggregation = new AggregationConfig();
        aggregation.setEnabled(true);
        aggregation.setEnableQps(false);
        aggregation.setBucketNum(100);

        MetricsBuilder builder = MetricsBuilder.newBuilder();
        builder.protocol("test")
                .enableJvm(true)
                .enableThreadPool(true)
                .enableRegistry(false)
                .enableMetadata(false)
                .exportMetricsService(true)
                .enableMetricsInit(false)
                .enableCollectorSync(true)
                .collectorSyncPeriod(10)
                .prometheus(prometheus)
                .histogram(histogram)
                .aggregation(aggregation)
                .exportServiceProtocol("tri")
                .exportServicePort(10010)
                .useGlobalRegistry(true)
                .enableRpc(true)
                .rpcLevel("METHOD");

        MetricsConfig config = builder.build();
        MetricsConfig config2 = builder.build();

        Assertions.assertEquals("test", config.getProtocol());
        Assertions.assertTrue(config.getEnableJvm());
        Assertions.assertTrue(config.getEnableThreadpool());
        Assertions.assertFalse(config.getEnableRegistry());
        Assertions.assertFalse(config.getEnableMetadata());
        Assertions.assertTrue(config.getExportMetricsService());
        Assertions.assertFalse(config.getEnableMetricsInit());
        Assertions.assertTrue(config.getEnableCollectorSync());
        Assertions.assertEquals(10, config.getCollectorSyncPeriod());
        Assertions.assertSame(prometheus, config.getPrometheus());
        Assertions.assertSame(exporter, config.getPrometheus().getExporter());
        Assertions.assertSame(pushGateway, config.getPrometheus().getPushgateway());
        Assertions.assertTrue(config.getPrometheus().getExporter().getEnabled());
        Assertions.assertTrue(config.getPrometheus().getExporter().getEnableHttpServiceDiscovery());
        Assertions.assertEquals(
                "localhost:8080", config.getPrometheus().getExporter().getHttpServiceDiscoveryUrl());
        Assertions.assertTrue(config.getPrometheus().getPushgateway().getEnabled());
        Assertions.assertEquals(
                "localhost:9091", config.getPrometheus().getPushgateway().getBaseUrl());
        Assertions.assertEquals(
                "username", config.getPrometheus().getPushgateway().getUsername());
        Assertions.assertEquals(
                "password", config.getPrometheus().getPushgateway().getPassword());
        Assertions.assertEquals("job", config.getPrometheus().getPushgateway().getJob());
        Assertions.assertEquals(30, config.getPrometheus().getPushgateway().getPushInterval());
        Assertions.assertSame(histogram, config.getHistogram());
        Assertions.assertTrue(config.getHistogram().getEnabled());
        Assertions.assertEquals(1000, config.getHistogram().getMaxExpectedMs());
        Assertions.assertArrayEquals(
                new Integer[] {100, 200, 300, 400, 500}, config.getHistogram().getBucketsMs());
        Assertions.assertArrayEquals(
                new double[] {0.5, 0.9, 0.95, 0.99}, config.getHistogram().getPercentiles());
        Assertions.assertSame(aggregation, config.getAggregation());
        Assertions.assertTrue(config.getAggregation().getEnabled());
        Assertions.assertFalse(config.getAggregation().getEnableQps());
        Assertions.assertEquals(100, config.getAggregation().getBucketNum());
        Assertions.assertEquals("tri", config.getExportServiceProtocol());
        Assertions.assertEquals(10010, config.getExportServicePort());
        Assertions.assertTrue(config.getUseGlobalRegistry());
        Assertions.assertTrue(config.getEnableRpc());
        Assertions.assertEquals("METHOD", config.getRpcLevel());
        Assertions.assertNotSame(config, config2);
    }
}
