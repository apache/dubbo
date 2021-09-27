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
import org.apache.dubbo.config.ConfigKeys;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigMode;
import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;

/**
 * Dubbo {@link ConfigurationProperties Config Properties} only used to generate JSON metadata(non-public class)
 *
 * @see ConfigKeys
 * @since 2.7.1
 */
@ConfigurationProperties(DUBBO_PREFIX)
public class DubboConfigurationProperties {

    @NestedConfigurationProperty
    private Config config = new Config();

    @NestedConfigurationProperty
    private Scan scan = new Scan();

    // Single Config Bindings
    @NestedConfigurationProperty
    private ApplicationConfig application = new ApplicationConfig();

    @NestedConfigurationProperty
    private ModuleConfig module = new ModuleConfig();

    @NestedConfigurationProperty
    private RegistryConfig registry = new RegistryConfig();

    @NestedConfigurationProperty
    private ProtocolConfig protocol = new ProtocolConfig();

    @NestedConfigurationProperty
    private MonitorConfig monitor = new MonitorConfig();

    @NestedConfigurationProperty
    private ProviderConfig provider = new ProviderConfig();

    @NestedConfigurationProperty
    private ConsumerConfig consumer = new ConsumerConfig();

    @NestedConfigurationProperty
    private ConfigCenterBean configCenter = new ConfigCenterBean();

    @NestedConfigurationProperty
    private MetadataReportConfig metadataReport = new MetadataReportConfig();

    @NestedConfigurationProperty
    private MetricsConfig metrics = new MetricsConfig();

    // Multiple Config Bindings

    private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    private Map<String, RegistryConfig> registries = new LinkedHashMap<>();

    private Map<String, ProtocolConfig> protocols = new LinkedHashMap<>();

    private Map<String, MonitorConfig> monitors = new LinkedHashMap<>();

    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    private Map<String, ConsumerConfig> consumers = new LinkedHashMap<>();

    private Map<String, ConfigCenterBean> configCenters = new LinkedHashMap<>();

    private Map<String, MetadataReportConfig> metadataReports = new LinkedHashMap<>();

    private Map<String, MetricsConfig> metricses = new LinkedHashMap<>();

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Scan getScan() {
        return scan;
    }

    public void setScan(Scan scan) {
        this.scan = scan;
    }

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

    public ConfigCenterBean getConfigCenter() {
        return configCenter;
    }

    public void setConfigCenter(ConfigCenterBean configCenter) {
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

    public Map<String, ConfigCenterBean> getConfigCenters() {
        return configCenters;
    }

    public void setConfigCenters(Map<String, ConfigCenterBean> configCenters) {
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

    static class Config {

        /**
         * Config processing mode
         * @see ConfigMode
         */
        private ConfigMode mode = ConfigMode.STRICT;

        /**
         * Indicates multiple properties binding from externalized configuration or not.
         */
        private boolean multiple = DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE;

        /**
         * The property name of override Dubbo config
         */
        private boolean override = DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE;

        public boolean isOverride() {
            return override;
        }

        public void setOverride(boolean override) {
            this.override = override;
        }

        public boolean isMultiple() {
            return multiple;
        }

        public void setMultiple(boolean multiple) {
            this.multiple = multiple;
        }

        public ConfigMode getMode() {
            return mode;
        }

        public void setMode(ConfigMode mode) {
            this.mode = mode;
        }
    }

    static class Scan {

        /**
         * The basePackages to scan , the multiple-value is delimited by comma
         *
         * @see EnableDubbo#scanBasePackages()
         */
        private Set<String> basePackages = new LinkedHashSet<>();

        public Set<String> getBasePackages() {
            return basePackages;
        }

        public void setBasePackages(Set<String> basePackages) {
            this.basePackages = basePackages;
        }
    }
}
