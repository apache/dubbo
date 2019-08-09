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

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.ofNullable;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.utils.ReflectUtils.getProperty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.config.Constants.PROTOCOLS_SUFFIX;
import static org.apache.dubbo.config.Constants.REGISTRIES_SUFFIX;

/**
 * TODO
 * Experimental API, should only being used internally at present.
 * <p>
 * Maybe we can consider open to end user in the following version by providing a fluent style builder.
 *
 * <pre>{@code
 *  public void class DubboBuilder() {
 *
 *      public static DubboBuilder create() {
 *          return new DubboBuilder();
 *      }
 *
 *      public DubboBuilder application(ApplicationConfig application) {
 *          ConfigManager.getInstance().addApplication(application);
 *          return this;
 *      }
 *
 *      ...
 *
 *      public void build() {
 *          // export all ServiceConfigs
 *          // refer all ReferenceConfigs
 *      }
 *  }
 *  }
 * </pre>
 * </p>
 * TODO
 * The properties defined here are duplicate with that in ReferenceConfig/ServiceConfig,
 * the properties here are currently only used for duplication check but are still not being used in the export/refer process yet.
 * Maybe we can remove the property definition in ReferenceConfig/ServiceConfig and only keep the setXxxConfig() as an entrance.
 * All workflow internally can rely on ConfigManager.
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    private volatile ModuleConfig module;
    private volatile ApplicationConfig application;
    private volatile MonitorConfig monitor;
    private volatile MetricsConfig metrics;

    private final Map<String, ProtocolConfig> protocols = new ConcurrentHashMap<>();
    private final Map<String, RegistryConfig> registries = new ConcurrentHashMap<>();
    private final Map<String, ProviderConfig> providers = new ConcurrentHashMap<>();
    private final Map<String, ConsumerConfig> consumers = new ConcurrentHashMap<>();
    private final Map<String, ConfigCenterConfig> configCenters = new ConcurrentHashMap<>();
    private final Map<String, MetadataReportConfig> metadataConfigs = new ConcurrentHashMap<>();
    private final Map<String, ServiceConfig<?>> serviceConfigs = new ConcurrentHashMap<>();
    private final Map<String, ReferenceConfig<?>> referenceConfigs = new ConcurrentHashMap<>();

    private final StampedLock lock = new StampedLock();

    public static ConfigManager getInstance() {
        return CONFIG_MANAGER;
    }

    private ConfigManager() {
    }

    // ApplicationConfig correlative methods

    public void setApplication(ApplicationConfig application) {
        if (application != null) {
            checkDuplicate(this.application, application);
            this.application = application;
        }
    }

    public Optional<ApplicationConfig> getApplication() {
        return ofNullable(application);
    }

    // MonitorConfig correlative methods

    public void setMonitor(MonitorConfig monitor) {
        if (monitor != null) {
            checkDuplicate(this.monitor, monitor);
            this.monitor = monitor;
        }
    }

    public Optional<MonitorConfig> getMonitor() {
        return ofNullable(monitor);
    }

    // ModuleConfig correlative methods

    public void setModule(ModuleConfig module) {
        if (module != null) {
            checkDuplicate(this.module, module);
            this.module = module;
        }
    }

    public Optional<ModuleConfig> getModule() {
        return ofNullable(module);
    }

    public void setMetrics(MetricsConfig metrics) {
        if (metrics != null) {
            checkDuplicate(this.metrics, metrics);
            this.metrics = metrics;
        }
    }

    public Optional<MetricsConfig> getMetrics() {
        return ofNullable(metrics);
    }

    // ConfigCenterConfig correlative methods

    public void addConfigCenter(ConfigCenterConfig configCenter) {
        addIfAbsent(configCenter, configCenters);
    }

    public void addConfigCenters(Iterable<ConfigCenterConfig> configCenters) {
        configCenters.forEach(this::addConfigCenter);
    }

    public ConfigCenterConfig getConfigCenter(String id) {
        return configCenters.get(id);
    }

    public Collection<ConfigCenterConfig> getConfigCenters() {
        return configCenters.values();
    }

    // MetadataReportConfig correlative methods

    public void addMetadataReport(MetadataReportConfig metadataReportConfig) {
        addIfAbsent(metadataReportConfig, metadataConfigs);
    }

    public void addMetadataReports(Iterable<MetadataReportConfig> metadataReportConfigs) {
        metadataReportConfigs.forEach(this::addMetadataReport);
    }

    public Collection<MetadataReportConfig> getMetadataConfigs() {
        return metadataConfigs.values();
    }

    // MetadataReportConfig correlative methods

    public void addProvider(ProviderConfig providerConfig) {
        addIfAbsent(providerConfig, providers);
    }

    public Optional<ProviderConfig> getProvider(String id) {
        return ofNullable(providers.get(id));
    }

    public Optional<ProviderConfig> getDefaultProvider() {
        return getProvider(DEFAULT_KEY);
    }

    public Collection<ProviderConfig> getProviders() {
        return providers.values();
    }

    // ConsumerConfig correlative methods

    public void addConsumer(ConsumerConfig consumerConfig) {
        addIfAbsent(consumerConfig, consumers);
    }

    public Optional<ConsumerConfig> getConsumer(String id) {
        return ofNullable(consumers.get(id));
    }

    public Optional<ConsumerConfig> getDefaultConsumer() {
        return getConsumer(DEFAULT_KEY);
    }

    public Collection<ConsumerConfig> getConsumers() {
        return consumers.values();
    }

    // ProtocolConfig correlative methods

    public void addProtocol(ProtocolConfig protocolConfig) {
        addIfAbsent(protocolConfig, protocols);
    }

    public void addProtocols(Iterable<ProtocolConfig> protocolConfigs) {
        if (protocolConfigs != null) {
            protocolConfigs.forEach(this::addProtocol);
        }
    }

    public Optional<ProtocolConfig> getProtocol(String id) {
        return ofNullable(protocols.get(id));
    }

    public List<ProtocolConfig> getDefaultProtocols() {
        return getDefaultConfigs(protocols);
    }

    public Collection<ProtocolConfig> getProtocols() {
        return protocols.values();
    }

    public Set<String> getProtocolIds() {
        Set<String> protocolIds = new HashSet<>();
        protocolIds.addAll(getSubProperties(Environment.getInstance()
                .getExternalConfigurationMap(), PROTOCOLS_SUFFIX));
        protocolIds.addAll(getSubProperties(Environment.getInstance()
                .getAppExternalConfigurationMap(), PROTOCOLS_SUFFIX));

        protocolIds.addAll(protocols.keySet());
        return unmodifiableSet(protocolIds);
    }


    // RegistryConfig correlative methods

    public void addRegistry(RegistryConfig registryConfig) {
        addIfAbsent(registryConfig, registries);
    }

    public void addRegistries(Iterable<RegistryConfig> registryConfigs) {
        if (registryConfigs != null) {
            registryConfigs.forEach(this::addRegistry);
        }
    }

    public Optional<RegistryConfig> getRegistry(String id) {
        return ofNullable(registries.get(id));
    }

    public List<RegistryConfig> getDefaultRegistries() {
        return getDefaultConfigs(registries);
    }

    public Collection<RegistryConfig> getRegistries() {
        return registries.values();
    }

    public Set<String> getRegistryIds() {
        Set<String> registryIds = new HashSet<>();
        registryIds.addAll(getSubProperties(Environment.getInstance().getExternalConfigurationMap(),
                REGISTRIES_SUFFIX));
        registryIds.addAll(getSubProperties(Environment.getInstance().getAppExternalConfigurationMap(),
                REGISTRIES_SUFFIX));

        registryIds.addAll(registries.keySet());
        return unmodifiableSet(registryIds);
    }

    // ServiceConfig correlative methods

    public void addService(ServiceConfig<?> serviceConfig) {
        addIfAbsent(serviceConfig, serviceConfigs);
    }

    public Collection<ServiceConfig<?>> getServiceConfigs() {
        return serviceConfigs.values();
    }

    public <T> ServiceConfig<T> getServiceConfig(String id) {
        return (ServiceConfig<T>) serviceConfigs.get(id);
    }

    // ReferenceConfig correlative methods

    public void addReference(ReferenceConfig<?> referenceConfig) {
        addIfAbsent(referenceConfig, referenceConfigs);
    }

    public Collection<ReferenceConfig<?>> getReferenceConfigs() {
        return referenceConfigs.values();
    }

    public <T> ReferenceConfig<T> getReferenceConfig(String id) {
        return (ReferenceConfig<T>) referenceConfigs.get(id);
    }

    protected static Set<String> getSubProperties(Map<String, String> properties, String prefix) {
        return properties.keySet().stream().filter(k -> k.contains(prefix)).map(k -> {
            k = k.substring(prefix.length());
            return k.substring(0, k.indexOf("."));
        }).collect(Collectors.toSet());
    }

    public void refreshAll() {
        write(() -> {
            // refresh all configs here,
            getApplication().ifPresent(ApplicationConfig::refresh);
            getMonitor().ifPresent(MonitorConfig::refresh);
            getModule().ifPresent(ModuleConfig::refresh);

            getProtocols().forEach(ProtocolConfig::refresh);
            getRegistries().forEach(RegistryConfig::refresh);
            getProviders().forEach(ProviderConfig::refresh);
            getConsumers().forEach(ConsumerConfig::refresh);
        });

    }


    // For test purpose
    public void clear() {
        write(() -> {
            this.application = null;
            this.monitor = null;
            this.module = null;
            this.registries.clear();
            this.protocols.clear();
            this.providers.clear();
            this.consumers.clear();
            this.configCenters.clear();
            this.metadataConfigs.clear();
        });
    }

    private <V> V write(Callable<V> callable) {
        V value = null;
        long stamp = lock.writeLock();
        try {
            value = callable.call();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlockWrite(stamp);
        }
        return value;
    }

    private void write(Runnable runnable) {
        write(() -> {
            runnable.run();
            return null;
        });
    }


    private <V> V read(Callable<V> callable) {
        long stamp = lock.tryOptimisticRead();

        boolean readLock = false;

        V value = null;

        try {
            readLock = !lock.validate(stamp);

            if (readLock) {
                stamp = lock.readLock();
            }
            value = callable.call();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if (readLock) {
                lock.unlockRead(stamp);
            }
        }

        return value;
    }

    private static void checkDuplicate(AbstractConfig oldOne, AbstractConfig newOne) {
        if (oldOne != null && !oldOne.equals(newOne)) {
            String configName = oldOne.getClass().getSimpleName();
            throw new IllegalStateException("Duplicate Config found for " + configName + ", you should use only one unique " + configName + " for one application.");
        }
    }

    private static Map<Class<? extends AbstractConfig>, Map<String, ? extends AbstractConfig>> newMap() {
        return new HashMap<>();
    }

    private static <C extends AbstractConfig> void addIfAbsent(C config, Map<String, C> configsMap) {

        if (config == null) {
            return;
        }

        String key = getId(config);

        C existedConfig = configsMap.get(key);

        if (existedConfig != null && !config.equals(existedConfig)) {
            if (logger.isWarnEnabled()) {
                String type = config.getClass().getSimpleName();
                logger.warn(String.format("Duplicate %s found, there already has one default %s or more than two %ss have the same id, " +
                        "you can try to give each %s a different id : %s", type, type, type, type, config));
            }
        } else {
            configsMap.put(key, config);
        }
    }

    static <C extends AbstractConfig> String getId(C config) {
        String id = config.getId();
        return isNotEmpty(id) ? id : isDefaultConfig(config) ?
                config.getClass().getSimpleName() + "#" + DEFAULT_KEY : null;
    }

    static <C extends AbstractConfig> boolean isDefaultConfig(C config) {
        Boolean isDefault = getProperty(config, "default");
        return isDefault == null || TRUE.equals(isDefault);
    }

    static <C extends AbstractConfig> List<C> getDefaultConfigs(Map<String, C> configsMap) {
        return configsMap.values()
                .stream()
                .filter(ConfigManager::isDefaultConfig)
                .collect(Collectors.toList());
    }
}
