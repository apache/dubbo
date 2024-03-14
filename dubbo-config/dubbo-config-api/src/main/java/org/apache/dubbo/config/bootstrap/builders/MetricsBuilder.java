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

/**
 * This is a builder for build {@link MetricsConfig}.
 */
public class MetricsBuilder extends AbstractBuilder<MetricsConfig, MetricsBuilder> {

    private String protocol;

    /**
     * Enable jvm metrics when collecting.
     */
    private Boolean enableJvm;

    /**
     * Enable threadpool metrics when collecting.
     */
    private Boolean enableThreadpool;

    /**
     * Enable registry metrics.
     */
    private Boolean enableRegistry;

    /**
     * Enable metadata metrics.
     */
    private Boolean enableMetadata;

    /**
     * Export metrics service.
     */
    private Boolean exportMetricsService;

    /**
     * Enable metrics init.
     */
    private Boolean enableMetricsInit;

    /**
     * Enable collector sync.
     */
    private Boolean enableCollectorSync;

    /**
     * Collector sync period.
     */
    private Integer collectorSyncPeriod;

    /**
     * The prometheus metrics config
     */
    private PrometheusConfig prometheus;

    /**
     * The metrics aggregation config
     */
    private AggregationConfig aggregation;

    private HistogramConfig histogram;

    private String exportServiceProtocol;

    private Integer exportServicePort;

    /**
     * Decide whether to use the global registry of the micrometer.
     */
    private Boolean useGlobalRegistry;

    /**
     * Enable rpc metrics.
     */
    private Boolean enableRpc;

    /**
     * The level of the metrics, the value can be "SERVICE", "METHOD", default is method.
     */
    private String rpcLevel;

    public static MetricsBuilder newBuilder() {
        return new MetricsBuilder();
    }

    public MetricsBuilder protocol(String protocol) {
        this.protocol = protocol;
        return getThis();
    }

    public MetricsBuilder enableJvm(Boolean enableJvm) {
        this.enableJvm = enableJvm;
        return getThis();
    }

    public MetricsBuilder enableThreadPool(Boolean enableThreadPool) {
        this.enableThreadpool = enableThreadPool;
        return getThis();
    }

    public MetricsBuilder enableRegistry(Boolean enableRegistry) {
        this.enableRegistry = enableRegistry;
        return getThis();
    }

    public MetricsBuilder enableMetadata(Boolean enableMetadata) {
        this.enableMetadata = enableMetadata;
        return getThis();
    }

    public MetricsBuilder exportMetricsService(Boolean exportMetricsService) {
        this.exportMetricsService = exportMetricsService;
        return getThis();
    }

    public MetricsBuilder enableMetricsInit(Boolean enableMetricsInit) {
        this.enableMetricsInit = enableMetricsInit;
        return getThis();
    }

    public MetricsBuilder enableCollectorSync(Boolean enableCollectorSync) {
        this.enableCollectorSync = enableCollectorSync;
        return getThis();
    }

    public MetricsBuilder collectorSyncPeriod(Integer collectorSyncPeriod) {
        this.collectorSyncPeriod = collectorSyncPeriod;
        return getThis();
    }

    public MetricsBuilder prometheus(PrometheusConfig prometheus) {
        this.prometheus = prometheus;
        return getThis();
    }

    public MetricsBuilder aggregation(AggregationConfig aggregation) {
        this.aggregation = aggregation;
        return getThis();
    }

    public MetricsBuilder histogram(HistogramConfig histogram) {
        this.histogram = histogram;
        return getThis();
    }

    public MetricsBuilder exportServiceProtocol(String exportServiceProtocol) {
        this.exportServiceProtocol = exportServiceProtocol;
        return getThis();
    }

    public MetricsBuilder exportServicePort(Integer exportServicePort) {
        this.exportServicePort = exportServicePort;
        return getThis();
    }

    public MetricsBuilder useGlobalRegistry(Boolean useGlobalRegistry) {
        this.useGlobalRegistry = useGlobalRegistry;
        return getThis();
    }

    public MetricsBuilder enableRpc(Boolean enableRpc) {
        this.enableRpc = enableRpc;
        return getThis();
    }

    public MetricsBuilder rpcLevel(String rpcLevel) {
        this.rpcLevel = rpcLevel;
        return getThis();
    }

    @Override
    public MetricsConfig build() {
        MetricsConfig metrics = new MetricsConfig();
        super.build(metrics);

        metrics.setProtocol(protocol);
        metrics.setEnableJvm(enableJvm);
        metrics.setEnableThreadpool(enableThreadpool);
        metrics.setEnableRegistry(enableRegistry);
        metrics.setEnableMetadata(enableMetadata);
        metrics.setExportMetricsService(exportMetricsService);
        metrics.setEnableMetricsInit(enableMetricsInit);
        metrics.setEnableCollectorSync(enableCollectorSync);
        metrics.setCollectorSyncPeriod(collectorSyncPeriod);
        metrics.setPrometheus(prometheus);
        metrics.setAggregation(aggregation);
        metrics.setHistogram(histogram);
        metrics.setExportServiceProtocol(exportServiceProtocol);
        metrics.setExportServicePort(exportServicePort);
        metrics.setUseGlobalRegistry(useGlobalRegistry);
        metrics.setEnableRpc(enableRpc);
        metrics.setRpcLevel(rpcLevel);
        return metrics;
    }

    @Override
    protected MetricsBuilder getThis() {
        return this;
    }
}
