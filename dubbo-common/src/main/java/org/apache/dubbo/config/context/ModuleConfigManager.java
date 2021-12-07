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

import org.apache.dubbo.common.extension.DisableInject;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;
import static org.apache.dubbo.config.AbstractConfig.getTagName;

/**
 * Manage configs of module
 */
public class ModuleConfigManager extends AbstractConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleConfigManager.class);

    private Map<String, AbstractInterfaceConfig> serviceConfigCache = new ConcurrentHashMap<>();
    private final ConfigManager applicationConfigManager;


    public ModuleConfigManager(ModuleModel moduleModel) {
        super(moduleModel, Arrays.asList(ModuleConfig.class, ServiceConfigBase.class, ReferenceConfigBase.class, ProviderConfig.class, ConsumerConfig.class));
        applicationConfigManager = moduleModel.getApplicationModel().getApplicationConfigManager();
    }

    // ModuleConfig correlative methods

    @DisableInject
    public void setModule(ModuleConfig module) {
        addConfig(module);
    }

    public Optional<ModuleConfig> getModule() {
        return ofNullable(getSingleConfig(getTagName(ModuleConfig.class)));
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

    @Override
    public void refreshAll() {
        // refresh all configs here
        getModule().ifPresent(ModuleConfig::refresh);
        getProviders().forEach(ProviderConfig::refresh);
        getConsumers().forEach(ConsumerConfig::refresh);

        getReferences().forEach(ReferenceConfigBase::refresh);
        getServices().forEach(ServiceConfigBase::refresh);
    }

    @Override
    public void clear() {
        super.clear();
        this.serviceConfigCache.clear();
    }


    @Override
    protected <C extends AbstractConfig> Optional<C> findDuplicatedConfig(Map<String, C> configsMap, C config) {
        // check duplicated configs
        // special check service and reference config by unique service name, speed up the processing of large number of instances
        if (config instanceof ReferenceConfigBase || config instanceof ServiceConfigBase) {
            C existedConfig = (C) findDuplicatedInterfaceConfig((AbstractInterfaceConfig) config);
            if (existedConfig != null) {
                return Optional.of(existedConfig);
            }
        } else {
            return super.findDuplicatedConfig(configsMap, config);
        }
        return Optional.empty();
    }

    /**
     * check duplicated ReferenceConfig/ServiceConfig
     *
     * @param config
     */
    private AbstractInterfaceConfig findDuplicatedInterfaceConfig(AbstractInterfaceConfig config) {
        String uniqueServiceName;
        Map<String, AbstractInterfaceConfig> configCache;
        if (config instanceof ReferenceConfigBase) {
            return null;
        } else if (config instanceof ServiceConfigBase) {
            ServiceConfigBase serviceConfig = (ServiceConfigBase) config;
            uniqueServiceName = serviceConfig.getUniqueServiceName();
            configCache = serviceConfigCache;
        } else {
            throw new IllegalArgumentException("Illegal type of parameter 'config' : " + config.getClass().getName());
        }

        AbstractInterfaceConfig prevConfig = configCache.putIfAbsent(uniqueServiceName, config);
        if (prevConfig != null) {
            if (prevConfig == config) {
                return prevConfig;
            }

            if (prevConfig.equals(config)) {
                // Is there any problem with ignoring duplicate and equivalent but different ReferenceConfig instances?
                if (logger.isWarnEnabled() && duplicatedConfigs.add(config)) {
                    logger.warn("Ignore duplicated and equal config: " + config);
                }
                return prevConfig;
            }

            String configType = config.getClass().getSimpleName();
            String msg = "Found multiple " + configType + "s with unique service name [" +
                uniqueServiceName + "], previous: " + prevConfig + ", later: " + config + ". " +
                "There can only be one instance of " + configType + " with the same triple (group, interface, version). " +
                "If multiple instances are required for the same interface, please use a different group or version.";

            if (logger.isWarnEnabled() && duplicatedConfigs.add(config)) {
                logger.warn(msg);
            }
            if (!this.ignoreDuplicatedInterface) {
                throw new IllegalStateException(msg);
            }
        }
        return prevConfig;
    }

    @Override
    public void loadConfigs() {
        // load dubbo.providers.xxx
        loadConfigsOfTypeFromProps(ProviderConfig.class);

        // load dubbo.consumers.xxx
        loadConfigsOfTypeFromProps(ConsumerConfig.class);

        // load dubbo.modules.xxx
        loadConfigsOfTypeFromProps(ModuleConfig.class);

        // check configs
        checkDefaultAndValidateConfigs(ProviderConfig.class);
        checkDefaultAndValidateConfigs(ConsumerConfig.class);
        checkDefaultAndValidateConfigs(ModuleConfig.class);
    }


    //
    // Delegate read application configs
    //

    public ConfigManager getApplicationConfigManager() {
        return applicationConfigManager;
    }

    @Override
    public <C extends AbstractConfig> Map<String, C> getConfigsMap(Class<C> cls) {
        if (isSupportConfigType(cls)) {
            return super.getConfigsMap(cls);
        } else {
            // redirect to application ConfigManager
            return applicationConfigManager.getConfigsMap(cls);
        }
    }

    @Override
    public <C extends AbstractConfig> Collection<C> getConfigs(Class<C> configType) {
        if (isSupportConfigType(configType)) {
            return super.getConfigs(configType);
        } else {
            return applicationConfigManager.getConfigs(configType);
        }
    }

    @Override
    public <T extends AbstractConfig> Optional<T> getConfig(Class<T> cls, String idOrName) {
        if (isSupportConfigType(cls)) {
            return super.getConfig(cls, idOrName);
        } else {
            return applicationConfigManager.getConfig(cls, idOrName);
        }
    }

    @Override
    public <C extends AbstractConfig> List<C> getDefaultConfigs(Class<C> cls) {
        if (isSupportConfigType(cls)) {
            return super.getDefaultConfigs(cls);
        } else {
            return applicationConfigManager.getDefaultConfigs(cls);
        }
    }

    public Optional<ApplicationConfig> getApplication() {
        return applicationConfigManager.getApplication();
    }

    public Optional<MonitorConfig> getMonitor() {
        return applicationConfigManager.getMonitor();
    }

    public Optional<MetricsConfig> getMetrics() {
        return applicationConfigManager.getMetrics();
    }

    public Optional<SslConfig> getSsl() {
        return applicationConfigManager.getSsl();
    }

    public Optional<Collection<ConfigCenterConfig>> getDefaultConfigCenter() {
        return applicationConfigManager.getDefaultConfigCenter();
    }

    public Optional<ConfigCenterConfig> getConfigCenter(String id) {
        return applicationConfigManager.getConfigCenter(id);
    }

    public Collection<ConfigCenterConfig> getConfigCenters() {
        return applicationConfigManager.getConfigCenters();
    }

    public Collection<MetadataReportConfig> getMetadataConfigs() {
        return applicationConfigManager.getMetadataConfigs();
    }

    public Collection<MetadataReportConfig> getDefaultMetadataConfigs() {
        return applicationConfigManager.getDefaultMetadataConfigs();
    }

    public Optional<ProtocolConfig> getProtocol(String idOrName) {
        return applicationConfigManager.getProtocol(idOrName);
    }

    public List<ProtocolConfig> getDefaultProtocols() {
        return applicationConfigManager.getDefaultProtocols();
    }

    public Collection<ProtocolConfig> getProtocols() {
        return applicationConfigManager.getProtocols();
    }

    public Optional<RegistryConfig> getRegistry(String id) {
        return applicationConfigManager.getRegistry(id);
    }

    public List<RegistryConfig> getDefaultRegistries() {
        return applicationConfigManager.getDefaultRegistries();
    }

    public Collection<RegistryConfig> getRegistries() {
        return applicationConfigManager.getRegistries();
    }
}
