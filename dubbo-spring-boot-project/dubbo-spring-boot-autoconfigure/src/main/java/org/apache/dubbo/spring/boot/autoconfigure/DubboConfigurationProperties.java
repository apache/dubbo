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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConfigKeys;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.TracingConfig;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;

/**
 * Dubbo {@link ConfigurationProperties Config Properties} only used to generate JSON metadata (non-public class)
 *
 * @see ConfigKeys
 * @since 2.7.1
 */
@ConfigurationProperties(DUBBO_PREFIX)
public class DubboConfigurationProperties {

    /**
     * Configuration properties for the application.
     */
    @NestedConfigurationProperty
    private ApplicationConfig application = new ApplicationConfig();

    /**
     * Configuration properties for the module.
     */
    @NestedConfigurationProperty
    private ModuleConfig module = new ModuleConfig();

    /**
     * Configuration properties for the registry.
     */
    @NestedConfigurationProperty
    private RegistryConfig registry = new RegistryConfig();

    /**
     * Configuration properties for the protocol.
     */
    @NestedConfigurationProperty
    private ProtocolConfig protocol = new ProtocolConfig();

    /**
     * Configuration properties for the monitor.
     */
    @NestedConfigurationProperty
    private MonitorConfig monitor = new MonitorConfig();

    /**
     * Configuration properties for the provider.
     */
    @NestedConfigurationProperty
    private ProviderConfig provider = new ProviderConfig();

    /**
     * Configuration properties for the consumer.
     */
    @NestedConfigurationProperty
    private ConsumerConfig consumer = new ConsumerConfig();

    /**
     * Configuration properties for the config center.
     */
    @NestedConfigurationProperty
    private ConfigCenterConfig configCenter = new ConfigCenterConfig();

    /**
     * Configuration properties for the metadata report.
     */
    @NestedConfigurationProperty
    private MetadataReportConfig metadataReport = new MetadataReportConfig();

    /**
     * Configuration properties for metrics.
     */
    @NestedConfigurationProperty
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * Configuration properties for tracing.
     */
    @NestedConfigurationProperty
    private TracingConfig tracing = new TracingConfig();

    /**
     * Configuration properties for ssl.
     */
    @NestedConfigurationProperty
    private SslConfig ssl = new SslConfig();

    // Multiple Config Bindings

    /**
     * Multiple configurations for Module.
     */
    private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    /**
     * Multiple configurations for Registry.
     */
    private Map<String, RegistryConfig> registries = new LinkedHashMap<>();

    /**
     * Multiple configurations for Protocol.
     */
    private Map<String, ProtocolConfig> protocols = new LinkedHashMap<>();

    /**
     * Multiple configurations for Monitor.
     */
    private Map<String, MonitorConfig> monitors = new LinkedHashMap<>();

    /**
     * Multiple configurations for Provider.
     */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    /**
     * Multiple configurations for Consumer.
     */
    private Map<String, ConsumerConfig> consumers = new LinkedHashMap<>();

    /**
     * Multiple configurations for ConfigCenterBean.
     */
    private Map<String, ConfigCenterConfig> configCenters = new LinkedHashMap<>();

    /**
     * Multiple configurations for MetadataReportConfig.
     */
    private Map<String, MetadataReportConfig> metadataReports = new LinkedHashMap<>();

    /**
     * Multiple configurations for MetricsConfig.
     */
    private Map<String, MetricsConfig> metricses = new LinkedHashMap<>();

    /**
     * Multiple configurations for TracingConfig.
     */
    private Map<String, TracingConfig> tracings = new LinkedHashMap<>();

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        this.application = application;
    }

    public ModuleConfig getModule() {
        return module;
    }

    public void setModule(ModuleConfig module) {
        this.module = module;
    }

    public RegistryConfig getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registry = registry;
    }

    public ProtocolConfig getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolConfig protocol) {
        this.protocol = protocol;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
    }

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }

    public ConsumerConfig getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerConfig consumer) {
        this.consumer = consumer;
    }

    public ConfigCenterConfig getConfigCenter() {
        return configCenter;
    }

    public void setConfigCenter(ConfigCenterConfig configCenter) {
        this.configCenter = configCenter;
    }

    public MetadataReportConfig getMetadataReport() {
        return metadataReport;
    }

    public void setMetadataReport(MetadataReportConfig metadataReport) {
        this.metadataReport = metadataReport;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    public TracingConfig getTracing() {
        return tracing;
    }

    public void setTracing(TracingConfig tracing) {
        this.tracing = tracing;
    }

    public SslConfig getSsl() {
        return ssl;
    }

    public void setSsl(SslConfig ssl) {
        this.ssl = ssl;
    }

    public Map<String, ModuleConfig> getModules() {
        return modules;
    }

    public void setModules(Map<String, ModuleConfig> modules) {
        this.modules = modules;
    }

    public Map<String, RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(Map<String, RegistryConfig> registries) {
        this.registries = registries;
    }

    public Map<String, ProtocolConfig> getProtocols() {
        return protocols;
    }

    public void setProtocols(Map<String, ProtocolConfig> protocols) {
        this.protocols = protocols;
    }

    public Map<String, MonitorConfig> getMonitors() {
        return monitors;
    }

    public void setMonitors(Map<String, MonitorConfig> monitors) {
        this.monitors = monitors;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers;
    }

    public Map<String, ConsumerConfig> getConsumers() {
        return consumers;
    }

    public void setConsumers(Map<String, ConsumerConfig> consumers) {
        this.consumers = consumers;
    }

    public Map<String, ConfigCenterConfig> getConfigCenters() {
        return configCenters;
    }

    public void setConfigCenters(Map<String, ConfigCenterConfig> configCenters) {
        this.configCenters = configCenters;
    }

    public Map<String, MetadataReportConfig> getMetadataReports() {
        return metadataReports;
    }

    public void setMetadataReports(Map<String, MetadataReportConfig> metadataReports) {
        this.metadataReports = metadataReports;
    }

    public Map<String, MetricsConfig> getMetricses() {
        return metricses;
    }

    public void setMetricses(Map<String, MetricsConfig> metricses) {
        this.metricses = metricses;
    }

    public Map<String, TracingConfig> getTracings() {
        return tracings;
    }

    public void setTracings(Map<String, TracingConfig> tracings) {
        this.tracings = tracings;
    }
}
