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

import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.context.FrameworkExt;
import org.apache.dubbo.common.extension.DisableInject;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConfigKeys;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Optional.ofNullable;
import static org.apache.dubbo.config.AbstractConfig.getTagName;

/**
 * A lock-free config manager (through ConcurrentHashMap), for fast read operation.
 * The Write operation lock with sub configs map of config type, for safely check and add new config.
 */
public class ConfigManager extends AbstractConfigManager implements FrameworkExt {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    public static final String NAME = "config";
    public static final String BEAN_NAME = "dubboConfigManager";
    public static final String DUBBO_CONFIG_MODE = ConfigKeys.DUBBO_CONFIG_MODE;

    private ConfigMode configMode = ConfigMode.STRICT;

    private ApplicationModel applicationModel;

    private AtomicBoolean inited = new AtomicBoolean(false);

    private static Set<Class<? extends AbstractConfig>> uniqueConfigTypes = new ConcurrentHashSet<>();

    static {
        // init unique config types
        uniqueConfigTypes.add(ApplicationConfig.class);
        uniqueConfigTypes.add(ModuleConfig.class);
        uniqueConfigTypes.add(MonitorConfig.class);
        uniqueConfigTypes.add(MetricsConfig.class);
        uniqueConfigTypes.add(SslConfig.class);
    }

    public ConfigManager(ApplicationModel applicationModel) {
        super(applicationModel, Arrays.asList(ApplicationConfig.class, ModuleConfig.class, MonitorConfig.class,
            MetricsConfig.class, SslConfig.class, ProtocolConfig.class, RegistryConfig.class, ConfigCenterConfig.class,
            MetadataReportConfig.class));
        this.applicationModel = applicationModel;
    }

    @Override
    public void initialize() throws IllegalStateException {
        if (!inited.compareAndSet(false, true)) {
            return;
        }
        CompositeConfiguration configuration = applicationModel.getApplicationEnvironment().getConfiguration();
        String configModeStr = (String) configuration.getProperty(DUBBO_CONFIG_MODE);
        try {
            if (StringUtils.hasText(configModeStr)) {
                this.configMode = ConfigMode.valueOf(configModeStr.toUpperCase());
            }
        } catch (Exception e) {
            String msg = "Illegal '" + DUBBO_CONFIG_MODE + "' config value [" + configModeStr + "], available values " + Arrays.toString(ConfigMode.values());
            logger.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }

        logger.info("Config settings - config mode: " + configMode);
    }


// ApplicationConfig correlative methods

    /**
     * Set application config
     *
     * @param application
     * @return current application config instance
     */
    @DisableInject
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

    @DisableInject
    public void setMonitor(MonitorConfig monitor) {
        addConfig(monitor);
    }

    public Optional<MonitorConfig> getMonitor() {
        return ofNullable(getSingleConfig(getTagName(MonitorConfig.class)));
    }

    // ModuleConfig correlative methods

    @DisableInject
    public void setModule(ModuleConfig module) {
        addConfig(module);
    }

    public Optional<ModuleConfig> getModule() {
        return ofNullable(getSingleConfig(getTagName(ModuleConfig.class)));
    }

    @DisableInject
    public void setMetrics(MetricsConfig metrics) {
        addConfig(metrics);
    }

    public Optional<MetricsConfig> getMetrics() {
        return ofNullable(getSingleConfig(getTagName(MetricsConfig.class)));
    }

    @DisableInject
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

    public Collection<MetadataReportConfig> getDefaultMetadataConfigs() {
        Collection<MetadataReportConfig> defaults = getDefaultConfigs(getConfigsMap(getTagName(MetadataReportConfig.class)));
        if (CollectionUtils.isEmpty(defaults)) {
            return getMetadataConfigs();
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

    public List<RegistryConfig> getDefaultRegistries() {
        return getDefaultConfigs(getConfigsMap(getTagName(RegistryConfig.class)));
    }

    public Collection<RegistryConfig> getRegistries() {
        return getConfigs(getTagName(RegistryConfig.class));
    }


    public void refreshAll() {
        // refresh all configs here,
        getApplication().ifPresent(ApplicationConfig::refresh);
        getMonitor().ifPresent(MonitorConfig::refresh);
        getModule().ifPresent(ModuleConfig::refresh);
        getMetrics().ifPresent(MetricsConfig::refresh);
        getSsl().ifPresent(SslConfig::refresh);

        getProtocols().forEach(ProtocolConfig::refresh);
        getRegistries().forEach(RegistryConfig::refresh);
        getConfigCenters().forEach(ConfigCenterConfig::refresh);
        getMetadataConfigs().forEach(MetadataReportConfig::refresh);
    }

    private boolean isUniqueConfig(AbstractConfig config) {
        if (uniqueConfigTypes.contains(config.getClass())) {
            return true;
        }
        for (Class<? extends AbstractConfig> uniqueConfigType : uniqueConfigTypes) {
            if (uniqueConfigType.isAssignableFrom(config.getClass())) {
                return true;
            }
        }
        return false;
    }

    protected <C extends AbstractConfig> C getSingleConfig(String configType) throws IllegalStateException {
        Map<String, AbstractConfig> configsMap = getConfigsMap(configType);
        int size = configsMap.size();
        if (size < 1) {
//                throw new IllegalStateException("No such " + configType.getName() + " is found");
            return null;
        } else if (size > 1) {
            throw new IllegalStateException("Expected single instance of " + configType + ", but found " + size +
                " instances, please remove redundant configs. instances: " + configsMap.values());
        }
        return (C) configsMap.values().iterator().next();
    }

    @Override
    protected <C extends AbstractConfig> Optional<C> findDuplicatedConfig(Map<String, C> configsMap, C config) {

        // find by value
        Optional<C> prevConfig = findConfigByValue(configsMap.values(), config);
        if (prevConfig.isPresent()) {
            if (prevConfig.get() == config) {
                // the new one is same as existing one
                return prevConfig;
            }

            // ignore duplicated equivalent config
            if (logger.isInfoEnabled() && duplicatedConfigs.add(config)) {
                logger.info("Ignore duplicated config: " + config);
            }
            return prevConfig;
        }

        // check unique config
        if (configsMap.size() > 0 && isUniqueConfig(config)) {
            C oldOne = configsMap.values().iterator().next();
            String configName = oldOne.getClass().getSimpleName();
            String msgPrefix = "Duplicate Configs found for " + configName + ", only one unique " + configName +
                " is allowed for one application. previous: " + oldOne + ", later: " + config + ". According to config mode [" + configMode + "], ";
            switch (configMode) {
                case STRICT: {
                    if (!isEquals(oldOne, config)) {
                        throw new IllegalStateException(msgPrefix + "please remove redundant configs and keep only one.");
                    }
                    break;
                }
                case IGNORE: {
                    // ignore later config
                    if (logger.isWarnEnabled() && duplicatedConfigs.add(config)) {
                        logger.warn(msgPrefix + "keep previous config and ignore later config");
                    }
                    return Optional.of(oldOne);
                }
                case OVERRIDE: {
                    // clear previous config, add new config
                    configsMap.clear();
                    if (logger.isWarnEnabled() && duplicatedConfigs.add(config)) {
                        logger.warn(msgPrefix + "override previous config with later config");
                    }
                    break;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void loadConfigs() {
        // application config has load before starting config center
        // load dubbo.applications.xxx
        loadConfigsOfTypeFromProps(ApplicationConfig.class);

        // load dubbo.modules.xxx
        loadConfigsOfTypeFromProps(ModuleConfig.class);

        // load dubbo.monitors.xxx
        loadConfigsOfTypeFromProps(MonitorConfig.class);

        // load dubbo.metricses.xxx
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

        checkConfigs();
    }

    private void checkConfigs() {
        // check config types (ignore metadata-center)
        List<Class<? extends AbstractConfig>> multipleConfigTypes = Arrays.asList(
            ApplicationConfig.class,
            ProtocolConfig.class,
            RegistryConfig.class,
            MetadataReportConfig.class,
            MonitorConfig.class,
            ModuleConfig.class,
            MetricsConfig.class,
            SslConfig.class);

        for (Class<? extends AbstractConfig> configType : multipleConfigTypes) {
            checkDefaultAndValidateConfigs(configType);
        }

        // check port conflicts
        Map<Integer, ProtocolConfig> protocolPortMap = new LinkedHashMap<>();
        for (ProtocolConfig protocol : this.getProtocols()) {
            Integer port = protocol.getPort();
            if (port == null || port == -1) {
                continue;
            }
            ProtocolConfig prevProtocol = protocolPortMap.get(port);
            if (prevProtocol != null) {
                throw new IllegalStateException("Duplicated port used by protocol configs, port: " + port +
                    ", configs: " + Arrays.asList(prevProtocol, protocol));
            }
            protocolPortMap.put(port, protocol);
        }
    }

    ConfigMode getConfigMode() {
        return configMode;
    }
}
