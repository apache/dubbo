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
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.config.nested.HistogramConfig;
import org.apache.dubbo.config.nested.OtlpMetricConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;
import org.apache.dubbo.config.support.Nested;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the metrics.
 */
public class MetricsConfig extends AbstractConfig {

    private static final long serialVersionUID = -9089919311611546383L;

    /**
     * Protocol used for metrics.
     */
    private String protocol;

    /**
     * Whether to enable JVM metrics collection.
     */
    private Boolean enableJvm;

    /**
     * Whether to enable thread pool metrics collection.
     */
    private Boolean enableThreadpool;

    /**
     * Whether to enable registry metrics collection.
     */
    private Boolean enableRegistry;

    /**
     * Whether to enable metadata metrics collection.
     */
    private Boolean enableMetadata;

    /**
     * Whether to export metrics service.
     */
    private Boolean exportMetricsService;

    /**
     * Whether to enable Netty metrics collection.
     */
    private Boolean enableNetty;

    /**
     * Whether to enable metrics initialization.
     */
    private Boolean enableMetricsInit;

    /**
     * Whether to enable collector synchronization.
     */
    private Boolean enableCollectorSync;

    /**
     * Collector synchronization period.
     */
    private Integer collectorSyncPeriod;

    /**
     * Configuration for Prometheus metrics collection.
     */
    @Nested
    private PrometheusConfig prometheus;

    /**
     * Configuration for metrics aggregation.
     */
    @Nested
    private AggregationConfig aggregation;

    /**
     * Configuration for metrics histogram.
     */
    @Nested
    private HistogramConfig histogram;

    /**
     * Protocol used for metrics collection and export.
     */
    @Nested
    private OtlpMetricConfig otlp;

    private String exportServiceProtocol;

    /**
     * Port used for exporting metrics services.
     */
    private Integer exportServicePort;

    /**
     * Decide whether to use the global registry of Micrometer.
     */
    private Boolean useGlobalRegistry;

    /**
     * Whether to enable RPC (Remote Procedure Call) metrics collection.
     */
    private Boolean enableRpc;

    /**
     * The level of metrics collection, which can be "SERVICE" or "METHOD". The default is "METHOD".
     */
    private String rpcLevel;

    public MetricsConfig() {}

    public MetricsConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    public URL toUrl() {
        Map<String, String> map = new HashMap<>();
        appendParameters(map, this);

        // ignore address parameter, use specified url in each metrics server config
        // the address "localhost" here is meaningless
        URL url = UrlUtils.parseURL("localhost", map);
        url = url.setScopeModel(getScopeModel());
        return url;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Boolean getEnableJvm() {
        return enableJvm;
    }

    public String getRpcLevel() {
        return rpcLevel;
    }

    public void setRpcLevel(String rpcLevel) {
        this.rpcLevel = rpcLevel;
    }

    public void setEnableJvm(Boolean enableJvm) {
        this.enableJvm = enableJvm;
    }

    public Boolean getEnableRegistry() {
        return enableRegistry;
    }

    public void setEnableRegistry(Boolean enableRegistry) {
        this.enableRegistry = enableRegistry;
    }

    public PrometheusConfig getPrometheus() {
        return prometheus;
    }

    public void setPrometheus(PrometheusConfig prometheus) {
        this.prometheus = prometheus;
    }

    public AggregationConfig getAggregation() {
        return aggregation;
    }

    public void setAggregation(AggregationConfig aggregation) {
        this.aggregation = aggregation;
    }

    public HistogramConfig getHistogram() {
        return histogram;
    }

    public void setHistogram(HistogramConfig histogram) {
        this.histogram = histogram;
    }

    public OtlpMetricConfig getOtlp() {
        return otlp;
    }

    public void setOtlp(OtlpMetricConfig otlp) {
        this.otlp = otlp;
    }

    public String getExportServiceProtocol() {
        return exportServiceProtocol;
    }

    public void setExportServiceProtocol(String exportServiceProtocol) {
        this.exportServiceProtocol = exportServiceProtocol;
    }

    public Integer getExportServicePort() {
        return exportServicePort;
    }

    public void setExportServicePort(Integer exportServicePort) {
        this.exportServicePort = exportServicePort;
    }

    public Boolean getEnableMetadata() {
        return enableMetadata;
    }

    public void setEnableMetadata(Boolean enableMetadata) {
        this.enableMetadata = enableMetadata;
    }

    public Boolean getExportMetricsService() {
        return exportMetricsService;
    }

    public void setExportMetricsService(Boolean exportMetricsService) {
        this.exportMetricsService = exportMetricsService;
    }

    public Boolean getEnableThreadpool() {
        return enableThreadpool;
    }

    public void setEnableThreadpool(Boolean enableThreadpool) {
        this.enableThreadpool = enableThreadpool;
    }

    public Boolean getEnableMetricsInit() {
        return enableMetricsInit;
    }

    public void setEnableMetricsInit(Boolean enableMetricsInit) {
        this.enableMetricsInit = enableMetricsInit;
    }

    public Boolean getEnableCollectorSync() {
        return enableCollectorSync;
    }

    public void setEnableCollectorSync(Boolean enableCollectorSync) {
        this.enableCollectorSync = enableCollectorSync;
    }

    public Integer getCollectorSyncPeriod() {
        return collectorSyncPeriod;
    }

    public void setCollectorSyncPeriod(Integer collectorSyncPeriod) {
        this.collectorSyncPeriod = collectorSyncPeriod;
    }

    public Boolean getUseGlobalRegistry() {
        return useGlobalRegistry;
    }

    public void setUseGlobalRegistry(Boolean useGlobalRegistry) {
        this.useGlobalRegistry = useGlobalRegistry;
    }

    public Boolean getEnableRpc() {
        return enableRpc;
    }

    public void setEnableRpc(Boolean enableRpc) {
        this.enableRpc = enableRpc;
    }

    public Boolean getEnableNetty() {
        return enableNetty;
    }

    public void setEnableNetty(Boolean enableNetty) {
        this.enableNetty = enableNetty;
    }
}
