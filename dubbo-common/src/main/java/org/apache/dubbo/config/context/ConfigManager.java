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
package org.apache.dubbo.config.context;

<<<<<<< HEAD
import org.apache.dubbo.common.context.FrameworkExt;
import org.apache.dubbo.common.context.LifecycleAdapter;
import org.apache.dubbo.common.extension.Inject;
=======
import org.apache.dubbo.common.context.ApplicationExt;
import org.apache.dubbo.common.extension.DisableInject;
>>>>>>> origin/3.2
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConfigKeys;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
<<<<<<< HEAD
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
=======

>>>>>>> origin/3.2
import static java.util.Optional.ofNullable;
import static org.apache.dubbo.config.AbstractConfig.getTagName;
<<<<<<< HEAD
import static org.apache.dubbo.config.Constants.PROTOCOLS_PREFIX;
import static org.apache.dubbo.config.Constants.REGISTRIES_PREFIX;
=======
>>>>>>> origin/3.2

/**
 * A lock-free config manager (through ConcurrentHashMap), for fast read operation.
 * The Write operation lock with sub configs map of config type, for safely check and add new config.
 */
public class ConfigManager extends AbstractConfigManager implements ApplicationExt {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    public static final String NAME = "config";
    public static final String BEAN_NAME = "dubboConfigManager";
    public static final String DUBBO_CONFIG_MODE = ConfigKeys.DUBBO_CONFIG_MODE;


    public ConfigManager(ApplicationModel applicationModel) {
        super(applicationModel, Arrays.asList(ApplicationConfig.class, MonitorConfig.class,
            MetricsConfig.class, SslConfig.class, ProtocolConfig.class, RegistryConfig.class, ConfigCenterConfig.class,
            MetadataReportConfig.class));
    }

<<<<<<< HEAD
    // ApplicationConfig correlative methods
    @Inject(enable = false)
=======

// ApplicationConfig correlative methods

    /**
     * Set application config
     *
     * @param application
     * @return current application config instance
     */
    @DisableInject
>>>>>>> origin/3.2
    public void setApplication(ApplicationConfig application) {
        addConfig(application);
    }

    public Optional<ApplicationConfig> getApplication() {
        return ofNullable(getSingleConfig(getTagName(ApplicationConfig.class)));
    }

    public ApplicationConfig getApplicationOrElseThrow() {
        return getApplication().orElseThrow(() -> new IllegalStateException("There's no ApplicationConfig specified."));
    }

    // MonitorConfig correlative methods

<<<<<<< HEAD
    @Inject(enable = false)
=======
    @DisableInject
>>>>>>> origin/3.2
    public void setMonitor(MonitorConfig monitor) {
        addConfig(monitor);
    }

    public Optional<MonitorConfig> getMonitor() {
<<<<<<< HEAD
        return ofNullable(getConfig(getTagName(MonitorConfig.class)));
    }

    // ModuleConfig correlative methods
    @Inject(enable = false)
    public void setModule(ModuleConfig module) {
        addConfig(module, true);
    }

    public Optional<ModuleConfig> getModule() {
        return ofNullable(getConfig(getTagName(ModuleConfig.class)));
    }

    @Inject(enable = false)
=======
        return ofNullable(getSingleConfig(getTagName(MonitorConfig.class)));
    }

    @DisableInject
>>>>>>> origin/3.2
    public void setMetrics(MetricsConfig metrics) {
        addConfig(metrics);
    }

    public Optional<MetricsConfig> getMetrics() {
        return ofNullable(getSingleConfig(getTagName(MetricsConfig.class)));
    }

<<<<<<< HEAD
    @Inject(enable = false)
=======
    @DisableInject
>>>>>>> origin/3.2
    public void setSsl(SslConfig sslConfig) {
        addConfig(sslConfig);
    }

    public Optional<SslConfig> getSsl() {
        return ofNullable(getSingleConfig(getTagName(SslConfig.class)));
    }

    // ConfigCenterConfig correlative methods

    public void addConfigCenter(ConfigCenterConfig configCenter) {
        addConfig(configCenter);
    }

    public void addConfigCenters(Iterable<ConfigCenterConfig> configCenters) {
        configCenters.forEach(this::addConfigCenter);
    }

    public Optional<Collection<ConfigCenterConfig>> getDefaultConfigCenter() {
        Collection<ConfigCenterConfig> defaults = getDefaultConfigs(getConfigsMap(getTagName(ConfigCenterConfig.class)));
        if (CollectionUtils.isEmpty(defaults)) {
            defaults = getConfigCenters();
        }
        return Optional.ofNullable(defaults);
    }

    public Optional<ConfigCenterConfig> getConfigCenter(String id) {
        return getConfig(ConfigCenterConfig.class, id);
    }

    public Collection<ConfigCenterConfig> getConfigCenters() {
        return getConfigs(getTagName(ConfigCenterConfig.class));
    }

    // MetadataReportConfig correlative methods

    public void addMetadataReport(MetadataReportConfig metadataReportConfig) {
        addConfig(metadataReportConfig);
    }

    public void addMetadataReports(Iterable<MetadataReportConfig> metadataReportConfigs) {
        metadataReportConfigs.forEach(this::addMetadataReport);
    }

    public Collection<MetadataReportConfig> getMetadataConfigs() {
        return getConfigs(getTagName(MetadataReportConfig.class));
    }

<<<<<<< HEAD
    public MetadataReportConfig getMetadataConfig(String id) {
        return getConfig(getTagName(MetadataReportConfig.class), id);
    }

=======
>>>>>>> origin/3.2
    public Collection<MetadataReportConfig> getDefaultMetadataConfigs() {
        Collection<MetadataReportConfig> defaults = getDefaultConfigs(getConfigsMap(getTagName(MetadataReportConfig.class)));
        if (CollectionUtils.isEmpty(defaults)) {
            return getMetadataConfigs();
<<<<<<< HEAD
        }
        return defaults;
    }

    // MetadataReportConfig correlative methods

    public void addProvider(ProviderConfig providerConfig) {
        addConfig(providerConfig);
    }

    public void addProviders(Iterable<ProviderConfig> providerConfigs) {
        providerConfigs.forEach(this::addProvider);
    }

    public Optional<ProviderConfig> getProvider(String id) {
        return ofNullable(getConfig(getTagName(ProviderConfig.class), id));
    }

    /**
     * Only allows one default ProviderConfig
     */
    public Optional<ProviderConfig> getDefaultProvider() {
        List<ProviderConfig> providerConfigs = getDefaultConfigs(getConfigsMap(getTagName(ProviderConfig.class)));
        if (CollectionUtils.isNotEmpty(providerConfigs)) {
            return Optional.of(providerConfigs.get(0));
        }
        return Optional.empty();
    }

    public Collection<ProviderConfig> getProviders() {
        return getConfigs(getTagName(ProviderConfig.class));
    }

    // ConsumerConfig correlative methods

    public void addConsumer(ConsumerConfig consumerConfig) {
        addConfig(consumerConfig);
    }

    public void addConsumers(Iterable<ConsumerConfig> consumerConfigs) {
        consumerConfigs.forEach(this::addConsumer);
    }

    public Optional<ConsumerConfig> getConsumer(String id) {
        return ofNullable(getConfig(getTagName(ConsumerConfig.class), id));
    }

    /**
     * Only allows one default ConsumerConfig
     */
    public Optional<ConsumerConfig> getDefaultConsumer() {
        List<ConsumerConfig> consumerConfigs = getDefaultConfigs(getConfigsMap(getTagName(ConsumerConfig.class)));
        if (CollectionUtils.isNotEmpty(consumerConfigs)) {
            return Optional.of(consumerConfigs.get(0));
=======
>>>>>>> origin/3.2
        }
        return defaults;
    }

    // ProtocolConfig correlative methods

    public void addProtocol(ProtocolConfig protocolConfig) {
        addConfig(protocolConfig);
    }

    public void addProtocols(Iterable<ProtocolConfig> protocolConfigs) {
        if (protocolConfigs != null) {
            protocolConfigs.forEach(this::addProtocol);
        }
    }

    public Optional<ProtocolConfig> getProtocol(String idOrName) {
        return getConfig(ProtocolConfig.class, idOrName);
    }

    public List<ProtocolConfig> getDefaultProtocols() {
        return getDefaultConfigs(ProtocolConfig.class);
    }

    @Override
    public <C extends AbstractConfig> List<C> getDefaultConfigs(Class<C> cls) {
        return getDefaultConfigs(getConfigsMap(getTagName(cls)));
    }

<<<<<<< HEAD
    public Set<String> getProtocolIds() {
        Set<String> protocolIds = new HashSet<>();
        protocolIds.addAll(getSubProperties(ApplicationModel.getEnvironment()
                .getExternalConfigurationMap(), PROTOCOLS_PREFIX));
        protocolIds.addAll(getSubProperties(ApplicationModel.getEnvironment()
                .getAppExternalConfigurationMap(), PROTOCOLS_PREFIX));

        return unmodifiableSet(protocolIds);
=======
    public Collection<ProtocolConfig> getProtocols() {
        return getConfigs(getTagName(ProtocolConfig.class));
>>>>>>> origin/3.2
    }


    // RegistryConfig correlative methods

    public void addRegistry(RegistryConfig registryConfig) {
        addConfig(registryConfig);
    }

    public void addRegistries(Iterable<RegistryConfig> registryConfigs) {
        if (registryConfigs != null) {
            registryConfigs.forEach(this::addRegistry);
        }
    }

    public Optional<RegistryConfig> getRegistry(String id) {
        return getConfig(RegistryConfig.class, id);
    }

    public List<RegistryConfig> getDefaultRegistries() {
        return getDefaultConfigs(getConfigsMap(getTagName(RegistryConfig.class)));
    }

    public Collection<RegistryConfig> getRegistries() {
        return getConfigs(getTagName(RegistryConfig.class));
    }

<<<<<<< HEAD
    public Set<String> getRegistryIds() {
        Set<String> registryIds = new HashSet<>();
        registryIds.addAll(getSubProperties(ApplicationModel.getEnvironment().getExternalConfigurationMap(),
                REGISTRIES_PREFIX));
        registryIds.addAll(getSubProperties(ApplicationModel.getEnvironment().getAppExternalConfigurationMap(),
                REGISTRIES_PREFIX));

        return unmodifiableSet(registryIds);
    }

    // ServiceConfig correlative methods
=======
>>>>>>> origin/3.2

    @Override
    public void refreshAll() {
        // refresh all configs here
        getApplication().ifPresent(ApplicationConfig::refresh);
        getMonitor().ifPresent(MonitorConfig::refresh);
        getMetrics().ifPresent(MetricsConfig::refresh);
        getSsl().ifPresent(SslConfig::refresh);

        getProtocols().forEach(ProtocolConfig::refresh);
        getRegistries().forEach(RegistryConfig::refresh);
        getConfigCenters().forEach(ConfigCenterConfig::refresh);
        getMetadataConfigs().forEach(MetadataReportConfig::refresh);
    }

    @Override
    public void loadConfigs() {
        // application config has load before starting config center
        // load dubbo.applications.xxx
        loadConfigsOfTypeFromProps(ApplicationConfig.class);

        // load dubbo.monitors.xxx
        loadConfigsOfTypeFromProps(MonitorConfig.class);

        // load dubbo.metrics.xxx
        loadConfigsOfTypeFromProps(MetricsConfig.class);

        // load multiple config types:
        // load dubbo.protocols.xxx
        loadConfigsOfTypeFromProps(ProtocolConfig.class);

        // load dubbo.registries.xxx
        loadConfigsOfTypeFromProps(RegistryConfig.class);

        // load dubbo.metadata-report.xxx
        loadConfigsOfTypeFromProps(MetadataReportConfig.class);

        // config centers has bean loaded before starting config center
        //loadConfigsOfTypeFromProps(ConfigCenterConfig.class);

        refreshAll();

        checkConfigs();

        // set model name
        if (StringUtils.isBlank(applicationModel.getModelName())) {
            applicationModel.setModelName(applicationModel.getApplicationName());
        }
    }

<<<<<<< HEAD
    public void clear() {
        write(this.configsCache::clear);
    }
=======
    private void checkConfigs() {
        // check config types (ignore metadata-center)
        List<Class<? extends AbstractConfig>> multipleConfigTypes = Arrays.asList(
            ApplicationConfig.class,
            ProtocolConfig.class,
            RegistryConfig.class,
            MonitorConfig.class,
            MetricsConfig.class,
            SslConfig.class);
>>>>>>> origin/3.2

        for (Class<? extends AbstractConfig> configType : multipleConfigTypes) {
            checkDefaultAndValidateConfigs(configType);
        }

<<<<<<< HEAD
    protected <C extends AbstractConfig> Map<String, C> getConfigsMap(String configType) {
        return (Map<String, C>) read(() -> configsCache.getOrDefault(configType, emptyMap()));
    }

    protected <C extends AbstractConfig> Collection<C> getConfigs(String configType) {
        return (Collection<C>) read(() -> getConfigsMap(configType).values());
    }

    protected <C extends AbstractConfig> C getConfig(String configType, String id) {
        return read(() -> {
            Map<String, C> configsMap = (Map) configsCache.getOrDefault(configType, emptyMap());
            return configsMap.get(id);
        });
    }

    protected <C extends AbstractConfig> C getConfig(String configType) throws IllegalStateException {
        return read(() -> {
            Map<String, C> configsMap = (Map) configsCache.getOrDefault(configType, emptyMap());
            int size = configsMap.size();
            if (size < 1) {
//                throw new IllegalStateException("No such " + configType.getName() + " is found");
                return null;
            } else if (size > 1) {

                AtomicReference<C> defaultConfig = new AtomicReference<>();
                configsMap.forEach((str, config) -> {
                    if (Boolean.TRUE.equals(config.isDefault())) {
                        defaultConfig.compareAndSet(null, config);
                    }
                });

                if (defaultConfig.get() != null) {
                    return defaultConfig.get();
                }

                logger.warn("Expected single matching of " + configType + ", but found " + size + " instances, will randomly pick the first one.");
=======
        // check port conflicts
        Map<Integer, ProtocolConfig> protocolPortMap = new LinkedHashMap<>();
        for (ProtocolConfig protocol : this.getProtocols()) {
            Integer port = protocol.getPort();
            if (port == null || port == -1) {
                continue;
>>>>>>> origin/3.2
            }
            ProtocolConfig prevProtocol = protocolPortMap.get(port);
            if (prevProtocol != null) {
                throw new IllegalStateException("Duplicated port used by protocol configs, port: " + port +
                    ", configs: " + Arrays.asList(prevProtocol, protocol));
            }
            protocolPortMap.put(port, protocol);
        }

        // Log the current configurations.
        logger.info("The current configurations or effective configurations are as follows:");
        for (Class<? extends AbstractConfig> configType : multipleConfigTypes) {
            getConfigs(configType).stream().forEach((config) -> {
                logger.info(config.toString());
            });
        }
    }

    public ConfigMode getConfigMode() {
        return configMode;
    }
}
