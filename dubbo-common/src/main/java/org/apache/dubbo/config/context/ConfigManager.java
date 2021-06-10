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

import org.apache.dubbo.common.context.FrameworkExt;
import org.apache.dubbo.common.context.LifecycleAdapter;
import org.apache.dubbo.common.extension.DisableInject;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.SslConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.config.AbstractConfig.getTagName;

public class ConfigManager extends LifecycleAdapter implements FrameworkExt {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    public static final String NAME = "config";
    public static final String BEAN_NAME = "dubboConfigManager";
    private static final String CONFIG_NAME_READ_METHOD = "getName";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    final Map<String, Map<String, AbstractConfig>> configsCache = newMap();

    private static Map<Class, AtomicInteger> configIdIndexes = new ConcurrentHashMap<>();

    private static volatile boolean configWarnLogEnabled = false;

    private static Set<Class<? extends AbstractConfig>> uniqueConfigTypes = new ConcurrentHashSet<>();

    static {
        // init unique config types
        uniqueConfigTypes.add(ApplicationConfig.class);
        uniqueConfigTypes.add(ModuleConfig.class);
        uniqueConfigTypes.add(MonitorConfig.class);
        uniqueConfigTypes.add(MetricsConfig.class);
        uniqueConfigTypes.add(SslConfig.class);
    }

    public ConfigManager() {
        try {
            String rawWarn = System.getProperty("dubbo.application.config.warn");
            if (rawWarn != null) {
                configWarnLogEnabled = Boolean.parseBoolean(rawWarn);
            }
        } catch (Exception e) {
            logger.warn("Illegal 'dubbo.application.config.warn' config, only boolean value is accepted.", e);
        }
    }

    // ApplicationConfig correlative methods

    /**
     * Set application config
     * @param application
     * @return current application config instance
     */
    @DisableInject
    public void setApplication(ApplicationConfig application) {
        addConfig(application, true);
    }

    public Optional<ApplicationConfig> getApplication() {
        return ofNullable(getSingleConfig(getTagName(ApplicationConfig.class)));
    }

    public ApplicationConfig getApplicationOrElseThrow() {
        return getApplication().orElseThrow(() -> new IllegalStateException("There's no ApplicationConfig specified."));
    }

    // MonitorConfig correlative methods

    @DisableInject
    public void setMonitor(MonitorConfig monitor) {
        addConfig(monitor, true);
    }

    public Optional<MonitorConfig> getMonitor() {
        return ofNullable(getSingleConfig(getTagName(MonitorConfig.class)));
    }

    // ModuleConfig correlative methods

    @DisableInject
    public void setModule(ModuleConfig module) {
        addConfig(module, true);
    }

    public Optional<ModuleConfig> getModule() {
        return ofNullable(getSingleConfig(getTagName(ModuleConfig.class)));
    }

    @DisableInject
    public void setMetrics(MetricsConfig metrics) {
        addConfig(metrics, true);
    }

    public Optional<MetricsConfig> getMetrics() {
        return ofNullable(getSingleConfig(getTagName(MetricsConfig.class)));
    }

    @DisableInject
    public void setSsl(SslConfig sslConfig) {
        addConfig(sslConfig, true);
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

    public Collection<MetadataReportConfig> getDefaultMetadataConfigs() {
        Collection<MetadataReportConfig> defaults = getDefaultConfigs(getConfigsMap(getTagName(MetadataReportConfig.class)));
        if (CollectionUtils.isEmpty(defaults)) {
            return getMetadataConfigs();
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
        return getConfig(ProviderConfig.class, id);
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
        return getConfig(ConsumerConfig.class, id);
    }

    /**
     * Only allows one default ConsumerConfig
     */
    public Optional<ConsumerConfig> getDefaultConsumer() {
        List<ConsumerConfig> consumerConfigs = getDefaultConfigs(getConfigsMap(getTagName(ConsumerConfig.class)));
        if (CollectionUtils.isNotEmpty(consumerConfigs)) {
            return Optional.of(consumerConfigs.get(0));
        }
        return Optional.empty();
    }

    public Collection<ConsumerConfig> getConsumers() {
        return getConfigs(getTagName(ConsumerConfig.class));
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

    public <C extends AbstractConfig> List<C> getDefaultConfigs(Class<C> cls) {
        return getDefaultConfigs(getConfigsMap(getTagName(cls)));
    }

    public Collection<ProtocolConfig> getProtocols() {
        return getConfigs(getTagName(ProtocolConfig.class));
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

    /**
     * Get config instance by id or by name
     * @param cls Config type
     * @param idOrName  the id or name of the config
     * @return
     */
    public <T extends AbstractConfig> Optional<T> getConfig(Class<T> cls, String idOrName) {
        T config = getConfigById(getTagName(cls), idOrName);
        if (config == null ) {
            config = getConfigByName(cls, idOrName);
        }
        return ofNullable(config);
    }

    public List<RegistryConfig> getDefaultRegistries() {
        return getDefaultConfigs(getConfigsMap(getTagName(RegistryConfig.class)));
    }

    public Collection<RegistryConfig> getRegistries() {
        return getConfigs(getTagName(RegistryConfig.class));
    }

    // ServiceConfig correlative methods

    public void addService(ServiceConfigBase<?> serviceConfig) {
        addConfig(serviceConfig);
    }

    public void addServices(Iterable<ServiceConfigBase<?>> serviceConfigs) {
        serviceConfigs.forEach(this::addService);
    }

    public Collection<ServiceConfigBase> getServices() {
        return getConfigs(getTagName(ServiceConfigBase.class));
    }

    public <T> ServiceConfigBase<T> getService(String id) {
        return getConfig(ServiceConfigBase.class, id).orElse(null);
    }

    // ReferenceConfig correlative methods

    public void addReference(ReferenceConfigBase<?> referenceConfig) {
        addConfig(referenceConfig);
    }

    public void addReferences(Iterable<ReferenceConfigBase<?>> referenceConfigs) {
        referenceConfigs.forEach(this::addReference);
    }

    public Collection<ReferenceConfigBase<?>> getReferences() {
        return getConfigs(getTagName(ReferenceConfigBase.class));
    }

    public <T> ReferenceConfigBase<T> getReference(String id) {
        return getConfig(ReferenceConfigBase.class, id).orElse(null);
    }

    public void refreshAll() {
        write(() -> {
            // refresh all configs here,
            getApplication().ifPresent(ApplicationConfig::refresh);
            getMonitor().ifPresent(MonitorConfig::refresh);
            getModule().ifPresent(ModuleConfig::refresh);
            getMetrics().ifPresent(MetricsConfig::refresh);
            getSsl().ifPresent(SslConfig::refresh);

            getProtocols().forEach(ProtocolConfig::refresh);
            getRegistries().forEach(RegistryConfig::refresh);
            getProviders().forEach(ProviderConfig::refresh);
            getConsumers().forEach(ConsumerConfig::refresh);
            getConfigCenters().forEach(ConfigCenterConfig::refresh);
            getMetadataConfigs().forEach(MetadataReportConfig::refresh);
        });

    }

    /**
     * In some scenario,  we may nee to add and remove ServiceConfig or ReferenceConfig dynamically.
     *
     * @param config the config instance to remove.
     */
    public void removeConfig(AbstractConfig config) {
        if (config == null) {
            return;
        }

        Map<String, AbstractConfig> configs = configsCache.get(getTagName(config.getClass()));
        if (CollectionUtils.isNotEmptyMap(configs)) {
            configs.values().removeIf(c -> config == c);
        }
    }

    public void clear() {
        write(() -> {
            this.configsCache.clear();
            configIdIndexes.clear();
        });
    }

    /**
     * @throws IllegalStateException
     * @since 2.7.8
     */
    @Override
    public void destroy() throws IllegalStateException {
        clear();
    }

    /**
     * Add the dubbo {@link AbstractConfig config}
     *
     * @param config the dubbo {@link AbstractConfig config}
     */
    public void addConfig(AbstractConfig config) {
        if (config == null) {
            return;
        }
        addConfig(config, isUniqueConfig(config));
    }

    private boolean isUniqueConfig(AbstractConfig config) {
        return uniqueConfigTypes.contains(config.getClass());
    }

    protected <T extends AbstractConfig> T addConfig(AbstractConfig config, boolean unique) {
        if (config == null) {
            return null;
        }
        // ignore MethodConfig
        if (config instanceof MethodConfig) {
            return null;
        }
        return (T) write(() -> {
            Map<String, AbstractConfig> configsMap = configsCache.computeIfAbsent(getTagName(config.getClass()), type -> newMap());
            return addIfAbsent(config, configsMap, unique);
        });
    }

    public <C extends AbstractConfig> Map<String, C> getConfigsMap(Class<C> cls) {
        return getConfigsMap(getTagName(cls));
    }

    private <C extends AbstractConfig> Map<String, C> getConfigsMap(String configType) {
        return (Map<String, C>) read(() -> configsCache.getOrDefault(configType, emptyMap()));
    }

    private <C extends AbstractConfig> Collection<C> getConfigs(String configType) {
        return (Collection<C>) read(() -> getConfigsMap(configType).values());
    }

    public <C extends AbstractConfig> Collection<C> getConfigs(Class<C> configType) {
        return (Collection<C>) read(() -> getConfigsMap(getTagName(configType)).values());
    }

    /**
     * Get config by id
     * @param configType
     * @param id
     * @return
     */
    private <C extends AbstractConfig> C getConfigById(String configType, String id) {
        return read(() -> {
            Map<String, C> configsMap = (Map) configsCache.getOrDefault(configType, emptyMap());
            return configsMap.get(id);
        });
    }

    /**
     * Get config by name if existed
     * @param cls
     * @param name
     * @return
     */
    private <C extends AbstractConfig> C getConfigByName(Class<? extends C> cls, String name) {
        return read(() -> {
            String configType = getTagName(cls);
            Map<String, C> configsMap = (Map) configsCache.getOrDefault(configType, emptyMap());
            if (configsMap.isEmpty()) {
                return null;
            }
            // try find config by name
            if (ReflectUtils.hasMethod(cls, CONFIG_NAME_READ_METHOD)) {
                List<C> list = configsMap.values().stream()
                        .filter(cfg -> name.equals(getConfigName(cfg)))
                        .collect(Collectors.toList());
                if (list.size() > 1) {
                    throw new IllegalStateException("Found more than one config by name: " + name +
                            ", instances: " + list + ". Please remove redundant configs or get config by id.");
                } else if (list.size() == 1) {
                    return list.get(0);
                }
            }
            return null;
        });
    }

    private <C extends AbstractConfig> String getConfigName(C config) {
        try {
            return (String) ReflectUtils.getProperty(config, CONFIG_NAME_READ_METHOD);
        } catch (Exception e) {
            return null;
        }
    }

    protected <C extends AbstractConfig> C getSingleConfig(String configType) throws IllegalStateException {
        return read(() -> {
            Map<String, C> configsMap = (Map) configsCache.getOrDefault(configType, emptyMap());
            int size = configsMap.size();
            if (size < 1) {
//                throw new IllegalStateException("No such " + configType.getName() + " is found");
                return null;
            } else if (size > 1) {
                throw new IllegalStateException("Expected single instance of " + configType + ", but found " + size +
                        " instances, please remove redundant configs. instances: "+configsMap.values());
            }

            return configsMap.values().iterator().next();
        });
    }

    private <V> V write(Callable<V> callable) {
        V value = null;
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            value = callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e.getCause());
        } finally {
            writeLock.unlock();
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
        Lock readLock = lock.readLock();
        V value = null;
        try {
            readLock.lock();
            value = callable.call();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
        return value;
    }

    private static void checkDuplicate(AbstractConfig oldOne, AbstractConfig newOne) throws IllegalStateException {
        if (!isEquals(oldOne, newOne)) {
            String configName = oldOne.getClass().getSimpleName();
            throw new IllegalStateException("Duplicate Configs found for " + configName + ", only one unique " + configName +
                    " is allowed for one application. old: "+oldOne+", new: "+newOne);
        }
    }

    private static boolean isEquals(AbstractConfig oldOne, AbstractConfig newOne) {
        if (oldOne == newOne) {
            return true;
        }
        if (oldOne == null || newOne == null) {
            return false;
        }
        if (oldOne.getClass() != newOne.getClass()) {
            return false;
        }
        // make both are refreshed or none is refreshed
        if (oldOne.isRefreshed() || newOne.isRefreshed()) {
            if (!oldOne.isRefreshed()) {
                oldOne.refresh();
            }
            if (!newOne.isRefreshed()) {
                newOne.refresh();
            }
        }
        return oldOne.equals(newOne);
    }

    private static Map newMap() {
        return new HashMap<>();
    }

    /**
     * Add config
     * @param config
     * @param configsMap
     * @param unique
     * @return the existing equivalent config or the new adding config
     * @throws IllegalStateException
     */
    static <C extends AbstractConfig> C addIfAbsent(C config, Map<String, C> configsMap, boolean unique)
            throws IllegalStateException {

        if (config == null || configsMap == null) {
            return config;
        }

        if (unique) { // check duplicate
            configsMap.values().forEach(c -> {
                checkDuplicate(c, config);
            });
        }

        // find by value
        // TODO Is there a problem with ignoring duplicate but different ReferenceConfig instances?
        Optional<C> prevConfig = configsMap.values().stream()
                .filter(val -> isEquals(val, config))
                .findFirst();
        if (prevConfig.isPresent()) {
            if (prevConfig.get() == config) {
                // the new one is same as existing one
                return prevConfig.get();
            }
            if (unique) {
                // unique config just need single instance, use prev equivalent instance is ok
                if (logger.isInfoEnabled()) {
                    logger.info("Ignore duplicated config: " + config);
                }
                return prevConfig.get();
            }

            // throw new IllegalStateException("An equivalent config instance already exists, please remove the redundant configuration. " +
            //        "prev: " + prevConfig.get() + ", new: " + config);
            // logger.warn("An equivalent config instance already exists, ignore the new instance. prev: " + prevConfig.get() +
            //        ", new: " + config);
            // return;
        }

        String key = getId(config);
            if (key == null) {
                // generate key for non-default config compatible with API usages
                key = generateConfigId(config);
            }

        C existedConfig = configsMap.get(key);

        if (isEquals(existedConfig, config)) {
            String type = config.getClass().getSimpleName();
            throw new IllegalStateException(String.format("Duplicate %s found, there already has one default %s or more than two %ss have the same id, " +
                    "you can try to give each %s a different id, key: %s, prev: %s, new: %s", type, type, type, type, key, existedConfig, config));
        } else {
            configsMap.put(key, config);
        }
        return config;
    }

    public static <C extends AbstractConfig> String generateConfigId(C config) {
        int idx = configIdIndexes.computeIfAbsent(config.getClass(), clazz -> new AtomicInteger(0)).incrementAndGet();
        return config.getClass().getSimpleName() + "#" + idx;
    }

    static <C extends AbstractConfig> String getId(C config) {
        String id = config.getId();
        return isNotEmpty(id) ? id : null;
    }

    static <C extends AbstractConfig> Boolean isDefaultConfig(C config) {
        return config.isDefault();
    }

    static <C extends AbstractConfig> List<C> getDefaultConfigs(Map<String, C> configsMap) {
        // find isDefault() == true
        List<C> list = configsMap.values()
                .stream()
                .filter(c -> TRUE.equals(ConfigManager.isDefaultConfig(c)))
                .collect(Collectors.toList());
        if (list.size() > 0) {
            return list;
        }

        // find isDefault() == null
        list = configsMap.values()
                .stream()
                .filter(c -> ConfigManager.isDefaultConfig(c) == null)
                .collect(Collectors.toList());
        return list;

        // exclude isDefault() == false
    }

}
