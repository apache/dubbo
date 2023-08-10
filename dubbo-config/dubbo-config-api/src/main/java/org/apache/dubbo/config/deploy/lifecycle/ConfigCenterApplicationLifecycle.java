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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.config.configcenter.wrapper.CompositeDynamicConfiguration;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.deploy.lifecycle.event.AppInitEvent;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.metrics.config.event.ConfigCenterEvent;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_INIT_CONFIG_CENTER;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

/**
 * Config-center lifecycle.
 */
@Activate(order = -3000)
public class ConfigCenterApplicationLifecycle implements ApplicationLifecycle {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ConfigCenterApplicationLifecycle.class);

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public void initialize(AppInitEvent initContext) {

        ApplicationModel applicationModel =initContext.getApplicationModel();

        ConfigManager configManager  = applicationModel.getApplicationConfigManager();

        // load config centers
        configManager.loadConfigsOfTypeFromProps(ConfigCenterConfig.class);

        useRegistryAsConfigCenterIfNecessary(applicationModel);

        // check Config Center
        Collection<ConfigCenterConfig> configCenters = configManager.getConfigCenters();
        if (CollectionUtils.isEmpty(configCenters)) {
            ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
            configCenterConfig.setScopeModel(applicationModel);
            configCenterConfig.refresh();
            ConfigValidationUtils.validateConfigCenterConfig(configCenterConfig);
            if (configCenterConfig.isValid()) {
                configManager.addConfigCenter(configCenterConfig);
                configCenters = configManager.getConfigCenters();
            }
        } else {
            for (ConfigCenterConfig configCenterConfig : configCenters) {
                configCenterConfig.refresh();
                ConfigValidationUtils.validateConfigCenterConfig(configCenterConfig);
            }
        }

        if (CollectionUtils.isNotEmpty(configCenters)) {

            Environment environment = applicationModel.modelEnvironment();
            CompositeDynamicConfiguration compositeDynamicConfiguration = new CompositeDynamicConfiguration();

            for (ConfigCenterConfig configCenter : configCenters) {
                // Pass config from ConfigCenterBean to applicationDeployer.getEnvironment()

                environment.updateExternalConfigMap(configCenter.getExternalConfiguration());
                environment.updateAppExternalConfigMap(configCenter.getAppExternalConfiguration());

                // Fetch config from remote config center
                compositeDynamicConfiguration.addConfiguration(prepareEnvironment(applicationModel,configCenter,environment));
            }
            environment.setDynamicConfiguration(compositeDynamicConfiguration);
        }
    }

    private DynamicConfiguration prepareEnvironment(ApplicationModel applicationModel,ConfigCenterConfig configCenter,Environment environment) {

        if (configCenter.isValid()) {
            if (!configCenter.checkOrUpdateInitialized(true)) {
                return null;
            }

            DynamicConfiguration dynamicConfiguration;
            try {
                dynamicConfiguration = getDynamicConfiguration(applicationModel,configCenter.toUrl());
            } catch (Exception e) {
                if (!configCenter.isCheck()) {
                    logger.warn(CONFIG_FAILED_INIT_CONFIG_CENTER, "", "", "The configuration center failed to initialize", e);
                    configCenter.setInitialized(false);
                    return null;
                } else {
                    throw new IllegalStateException(e);
                }
            }

            if (StringUtils.isNotEmpty(configCenter.getConfigFile())) {
                String configContent = dynamicConfiguration.getProperties(configCenter.getConfigFile(), configCenter.getGroup());
                if (StringUtils.isNotEmpty(configContent)) {
                    logger.info(String.format("Got global remote configuration from config center with key-%s and group-%s: \n %s", configCenter.getConfigFile(), configCenter.getGroup(), configContent));
                }
                String appGroup = applicationModel.getApplicationName();
                String appConfigContent = null;
                String appConfigFile = null;
                if (isNotEmpty(appGroup)) {
                    appConfigFile = isNotEmpty(configCenter.getAppConfigFile()) ? configCenter.getAppConfigFile() : configCenter.getConfigFile();
                    appConfigContent = dynamicConfiguration.getProperties(appConfigFile, appGroup);
                    if (StringUtils.isNotEmpty(appConfigContent)) {
                        logger.info(String.format("Got application specific remote configuration from config center with key %s and group %s: \n %s", appConfigFile, appGroup, appConfigContent));
                    }
                }
                try {
                    Map<String, String> configMap = parseProperties(configContent);
                    Map<String, String> appConfigMap = parseProperties(appConfigContent);

                    environment.updateExternalConfigMap(configMap);
                    environment.updateAppExternalConfigMap(appConfigMap);

                    // Add metrics
                    MetricsEventBus.publish(ConfigCenterEvent.toChangeEvent(applicationModel, configCenter.getConfigFile(), configCenter.getGroup(),
                        configCenter.getProtocol(), ConfigChangeType.ADDED.name(), configMap.size()));
                    if (isNotEmpty(appGroup)) {
                        MetricsEventBus.publish(ConfigCenterEvent.toChangeEvent(applicationModel, appConfigFile, appGroup,
                            configCenter.getProtocol(), ConfigChangeType.ADDED.name(), appConfigMap.size()));
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to parse configurations from Config Center.", e);
                }
            }
            return dynamicConfiguration;
        }
        return null;
    }

    /**
     * Get the instance of {@link DynamicConfiguration} by the specified connection {@link URL} of config-center
     *
     * @param connectionUrl of config-center
     * @return non-null
     * @since 2.7.5
     */
    private DynamicConfiguration getDynamicConfiguration(ApplicationModel applicationModel,URL connectionUrl) {
        String protocol = connectionUrl.getProtocol();

        DynamicConfigurationFactory factory = ConfigurationUtils.getDynamicConfigurationFactory(applicationModel, protocol);
        return factory.getDynamicConfiguration(connectionUrl);
    }

    /**
     * For compatibility purpose, use registry as the default config center when
     * there's no config center specified explicitly and
     * useAsConfigCenter of registryConfig is null or true
     */
    private void useRegistryAsConfigCenterIfNecessary(ApplicationModel applicationModel) {
        ConfigManager configManager = applicationModel.getApplicationConfigManager();

        // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
        if (applicationModel.modelEnvironment().getDynamicConfiguration().isPresent()) {
            return;
        }

        if (CollectionUtils.isNotEmpty(configManager.getConfigCenters())) {
            return;
        }

        // load registry
        configManager.loadConfigsOfTypeFromProps(RegistryConfig.class);

        List<RegistryConfig> defaultRegistries = configManager.getDefaultRegistries();
        if (defaultRegistries.size() > 0) {
            defaultRegistries
                .stream()
                .filter(registryConfig -> isUsedRegistryAsConfigCenter(applicationModel,registryConfig))
                .map(registryConfig -> registryAsConfigCenter(applicationModel,registryConfig))
                .forEach(configCenter -> {
                    if (configManager.getConfigCenter(configCenter.getId()).isPresent()) {
                        return;
                    }
                    configManager.addConfigCenter(configCenter);
                    logger.info("use registry as config-center: " + configCenter);

                });
        }
    }

    private boolean isUsedRegistryAsConfigCenter(ApplicationModel applicationModel,RegistryConfig registryConfig) {
        return isUsedRegistryAsCenter(applicationModel,registryConfig, registryConfig::getUseAsConfigCenter, "config",
            DynamicConfigurationFactory.class);
    }

    /**
     * Is used the specified registry as a center infrastructure
     *
     * @param registryConfig       the {@link RegistryConfig}
     * @param usedRegistryAsCenter the configured value on
     * @param centerType           the type name of center
     * @param extensionClass       an extension class of a center infrastructure
     * @return
     * @since 2.7.8
     */
    private boolean isUsedRegistryAsCenter(ApplicationModel applicationModel,
                                           RegistryConfig registryConfig, Supplier<Boolean> usedRegistryAsCenter,
                                           String centerType,
                                           Class<?> extensionClass) {
        final boolean supported;

        Boolean configuredValue = usedRegistryAsCenter.get();
        if (configuredValue != null) { // If configured, take its value.
            supported = configuredValue.booleanValue();
        } else {                       // Or check the extension existence
            String protocol = registryConfig.getProtocol();
            supported = supportsExtension(applicationModel,extensionClass, protocol);
            if (logger.isInfoEnabled()) {
                logger.info(format("No value is configured in the registry, the %s extension[name : %s] %s as the %s center"
                    , extensionClass.getSimpleName(), protocol, supported ? "supports" : "does not support", centerType));
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(format("The registry[%s] will be %s as the %s center", registryConfig,
                supported ? "used" : "not used", centerType));
        }
        return supported;
    }

    private ConfigCenterConfig registryAsConfigCenter(ApplicationModel applicationModel,RegistryConfig registryConfig) {

        String protocol = registryConfig.getProtocol();
        Integer port = registryConfig.getPort();
        URL url = URL.valueOf(registryConfig.getAddress(), registryConfig.getScopeModel());
        String id = "config-center-" + protocol + "-" + url.getHost() + "-" + port;

        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
        configCenterConfig.setId(id);
        configCenterConfig.setScopeModel(applicationModel);

        if (configCenterConfig.getParameters() == null) {
            configCenterConfig.setParameters(new HashMap<>());
        }
        if (CollectionUtils.isNotEmptyMap(registryConfig.getParameters())) {
            configCenterConfig.getParameters().putAll(registryConfig.getParameters()); // copy the parameters
        }

        configCenterConfig.getParameters().put(CLIENT_KEY, registryConfig.getClient());
        configCenterConfig.setProtocol(protocol);
        configCenterConfig.setPort(port);

        if (StringUtils.isNotEmpty(registryConfig.getGroup())) {
            configCenterConfig.setGroup(registryConfig.getGroup());
        }

        configCenterConfig.setAddress(getRegistryCompatibleAddress(registryConfig));
        configCenterConfig.setNamespace(registryConfig.getGroup());
        configCenterConfig.setUsername(registryConfig.getUsername());
        configCenterConfig.setPassword(registryConfig.getPassword());

        if (registryConfig.getTimeout() != null) {
            configCenterConfig.setTimeout(registryConfig.getTimeout().longValue());
        }
        configCenterConfig.setHighestPriority(false);
        return configCenterConfig;
    }

    private String getRegistryCompatibleAddress(RegistryConfig registryConfig) {
        String registryAddress = registryConfig.getAddress();
        String[] addresses = REGISTRY_SPLIT_PATTERN.split(registryAddress);
        if (ArrayUtils.isEmpty(addresses)) {
            throw new IllegalStateException("Invalid registry address found.");
        }
        String address = addresses[0];
        // since 2.7.8
        // Issue : https://github.com/apache/dubbo/issues/6476
        StringBuilder metadataAddressBuilder = new StringBuilder();
        URL url = URL.valueOf(address, registryConfig.getScopeModel());
        String protocolFromAddress = url.getProtocol();
        if (isEmpty(protocolFromAddress)) {
            // If the protocol from address is missing, is like :
            // "dubbo.registry.address = 127.0.0.1:2181"
            String protocolFromConfig = registryConfig.getProtocol();
            metadataAddressBuilder.append(protocolFromConfig).append("://");
        }
        metadataAddressBuilder.append(address);
        return metadataAddressBuilder.toString();
    }


    /**
     * Supports the extension with the specified class and name
     *
     * @param extensionClass the {@link Class} of extension
     * @param name           the name of extension
     * @return if supports, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    private boolean supportsExtension(ApplicationModel applicationModel,Class<?> extensionClass, String name) {
        if (isNotEmpty(name)) {
            ExtensionLoader<?> extensionLoader = applicationModel.getExtensionLoader(extensionClass);
            return extensionLoader.hasExtension(name);
        }
        return false;
    }

}
