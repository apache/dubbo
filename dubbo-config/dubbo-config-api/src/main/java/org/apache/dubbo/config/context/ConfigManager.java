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
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
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

    private final Map<Class<? extends AbstractConfig>, Map<String, AbstractConfig>> configsCache = newMap();

    private final StampedLock lock = new StampedLock();

    public static ConfigManager getInstance() {
        return CONFIG_MANAGER;
    }

    private ConfigManager() {
    }

    // ApplicationConfig correlative methods

    public void setApplication(ApplicationConfig application) {
        addConfig(application, true);
    }

    public Optional<ApplicationConfig> getApplication() {
        return ofNullable(getConfig(ApplicationConfig.class));
    }

    /**
     * Add the dubbo {@link AbstractConfig config}
     *
     * @param config the dubbo {@link AbstractConfig config}
     */
    public void addConfig(AbstractConfig config) {
        addConfig(config, false);
    }

    protected void addConfig(AbstractConfig config, boolean unique) {
        Class<? extends AbstractConfig> configType = config.getClass();
        write(() -> {
            Map<String, AbstractConfig> configsMap = configsCache.computeIfAbsent(configType, type -> newMap());
            addIfAbsent(config, configsMap, unique);
        });
    }

    protected <C extends AbstractConfig> Map<String, C> getConfigsMap(Class<? extends C> configType) {
        return read(() -> (Map) configsCache.getOrDefault(configType, emptyMap()));
    }

    protected <C extends AbstractConfig> Collection<C> getConfigs(Class<C> configType) {
        return read(() -> getConfigsMap(configType).values());
    }

    protected <C extends AbstractConfig> C getConfig(Class<C> configType, String id) {
        return read(() -> {
            Map<String, C> configsMap = (Map) configsCache.getOrDefault(configType, emptyMap());
            return configsMap.get(id);
        });
    }

    protected <C extends AbstractConfig> C getConfig(Class<C> configType) throws IllegalStateException {
        return read(() -> {
            Map<String, C> configsMap = (Map) configsCache.getOrDefault(configType, emptyMap());
            int size = configsMap.size();
            if (size < 0) {
//                throw new IllegalStateException("No such " + configType.getName() + " is found");
                return null;
            } else if (size > 1) {
                throw new IllegalStateException("The expected single matching " + configType.getName() + " but found " + size + " instances");
            } else {
                return configsMap.values().iterator().next();
            }
        });
    }

    // MonitorConfig correlative methods

    public void setMonitor(MonitorConfig monitor) {
        addConfig(monitor, true);
    }

    public Optional<MonitorConfig> getMonitor() {
        return ofNullable(getConfig(MonitorConfig.class));
    }

    // ModuleConfig correlative methods

    public void setModule(ModuleConfig module) {
        addConfig(module, true);

    }

    public Optional<ModuleConfig> getModule() {
        return ofNullable(getConfig(ModuleConfig.class));
    }

    public void setMetrics(MetricsConfig metrics) {
        addConfig(metrics, true);
    }

    public Optional<MetricsConfig> getMetrics() {
        return ofNullable(getConfig(MetricsConfig.class));
    }

    // ConfigCenterConfig correlative methods

    public void addConfigCenter(ConfigCenterConfig configCenter) {
        addConfig(configCenter);
    }

    public void addConfigCenters(Iterable<ConfigCenterConfig> configCenters) {
        configCenters.forEach(this::addConfigCenter);
    }

    public ConfigCenterConfig getConfigCenter(String id) {
        return getConfig(ConfigCenterConfig.class, id);
    }

    public Collection<ConfigCenterConfig> getConfigCenters() {
        return getConfigs(ConfigCenterConfig.class);
    }

    // MetadataReportConfig correlative methods

    public void addMetadataReport(MetadataReportConfig metadataReportConfig) {
        addConfig(metadataReportConfig);
    }

    public void addMetadataReports(Iterable<MetadataReportConfig> metadataReportConfigs) {
        metadataReportConfigs.forEach(this::addMetadataReport);
    }

    public Collection<MetadataReportConfig> getMetadataConfigs() {
        return getConfigs(MetadataReportConfig.class);
    }

    // MetadataReportConfig correlative methods

    public void addProvider(ProviderConfig providerConfig) {
        addConfig(providerConfig);
    }

    public Optional<ProviderConfig> getProvider(String id) {
        return ofNullable(getConfig(ProviderConfig.class, id));
    }

    public Optional<ProviderConfig> getDefaultProvider() {
        return getProvider(DEFAULT_KEY);
    }

    public Collection<ProviderConfig> getProviders() {
        return getConfigs(ProviderConfig.class);
    }

    // ConsumerConfig correlative methods

    public void addConsumer(ConsumerConfig consumerConfig) {
        addConfig(consumerConfig);
    }

    public Optional<ConsumerConfig> getConsumer(String id) {
        return ofNullable(getConfig(ConsumerConfig.class, id));
    }

    public Optional<ConsumerConfig> getDefaultConsumer() {
        return getConsumer(DEFAULT_KEY);
    }

    public Collection<ConsumerConfig> getConsumers() {
        return getConfigs(ConsumerConfig.class);
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

    public Optional<ProtocolConfig> getProtocol(String id) {
        return ofNullable(getConfig(ProtocolConfig.class, id));
    }

    public List<ProtocolConfig> getDefaultProtocols() {
        return getDefaultConfigs(getConfigsMap(ProtocolConfig.class));
    }

    public Collection<ProtocolConfig> getProtocols() {
        return getConfigs(ProtocolConfig.class);
    }

    public Set<String> getProtocolIds() {
        Set<String> protocolIds = new HashSet<>();
        protocolIds.addAll(getSubProperties(Environment.getInstance()
                .getExternalConfigurationMap(), PROTOCOLS_SUFFIX));
        protocolIds.addAll(getSubProperties(Environment.getInstance()
                .getAppExternalConfigurationMap(), PROTOCOLS_SUFFIX));

        protocolIds.addAll(getConfigIds(ProtocolConfig.class));
        return unmodifiableSet(protocolIds);
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
        return ofNullable(getConfig(RegistryConfig.class, id));
    }

    public List<RegistryConfig> getDefaultRegistries() {
        return getDefaultConfigs(getConfigsMap(RegistryConfig.class));
    }

    public Collection<RegistryConfig> getRegistries() {
        return getConfigs(RegistryConfig.class);
    }

    public Set<String> getRegistryIds() {
        Set<String> registryIds = new HashSet<>();
        registryIds.addAll(getSubProperties(Environment.getInstance().getExternalConfigurationMap(),
                REGISTRIES_SUFFIX));
        registryIds.addAll(getSubProperties(Environment.getInstance().getAppExternalConfigurationMap(),
                REGISTRIES_SUFFIX));

        registryIds.addAll(getConfigIds(RegistryConfig.class));
        return unmodifiableSet(registryIds);
    }

    // ServiceConfig correlative methods

    public void addService(ServiceConfig<?> serviceConfig) {
        addConfig(serviceConfig);
    }

    public Collection<ServiceConfig> getServiceConfigs() {
        return getConfigs(ServiceConfig.class);
    }

    public <T> ServiceConfig<T> getServiceConfig(String id) {
        return getConfig(ServiceConfig.class, id);
    }

    // ReferenceConfig correlative methods

    public void addReference(ReferenceConfig<?> referenceConfig) {
        addConfig(referenceConfig);
    }

    public Collection<ReferenceConfig> getReferenceConfigs() {
        return getConfigs(ReferenceConfig.class);
    }

    public <T> ReferenceConfig<T> getReferenceConfig(String id) {
        return getConfig(ReferenceConfig.class, id);
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
            this.configsCache.clear();
        });
    }

    private <C extends AbstractConfig> Collection<String> getConfigIds(Class<C> configType) {
        return getConfigs(configType)
                .stream()
                .map(AbstractConfig::getId)
                .collect(Collectors.toSet());
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

    private static void checkDuplicate(AbstractConfig oldOne, AbstractConfig newOne) throws IllegalStateException {
        if (oldOne != null && !oldOne.equals(newOne)) {
            String configName = oldOne.getClass().getSimpleName();
            throw new IllegalStateException("Duplicate Config found for " + configName + ", you should use only one unique " + configName + " for one application.");
        }
    }

    private static Map newMap() {
        return new HashMap<>();
    }

    static <C extends AbstractConfig> void addIfAbsent(C config, Map<String, C> configsMap, boolean unique)
            throws IllegalStateException {

        if (config == null || configsMap == null) {
            return;
        }

        if (unique) { // check duplicate
            configsMap.values().forEach(c -> {
                checkDuplicate(c, config);
            });
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
