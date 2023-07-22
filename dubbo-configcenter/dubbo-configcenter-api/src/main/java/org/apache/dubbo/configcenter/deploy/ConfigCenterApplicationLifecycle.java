package org.apache.dubbo.configcenter.deploy;

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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.metrics.config.event.ConfigCenterEvent;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_INIT_CONFIG_CENTER;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

/**
 * Config-center package life cycle manager.
 */
@Activate
public class ConfigCenterApplicationLifecycle implements ApplicationLifecycle {

    private static final String NAME = "config_center";

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ConfigCenterApplicationLifecycle.class);

    private DefaultApplicationDeployer applicationDeployer;

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.applicationDeployer = defaultApplicationDeployer;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize() {
        // load application config
        applicationDeployer.getConfigManager().loadConfigsOfTypeFromProps(ApplicationConfig.class);

        // try set model name
        if (StringUtils.isBlank(applicationDeployer.getApplicationModel().getModelName())) {
            applicationDeployer.getApplicationModel().setModelName(applicationDeployer.getApplicationModel().tryGetApplicationName());
        }

        // load config centers
        applicationDeployer.getConfigManager().loadConfigsOfTypeFromProps(ConfigCenterConfig.class);

        useRegistryAsConfigCenterIfNecessary();

        // check Config Center
        Collection<ConfigCenterConfig> configCenters = applicationDeployer.getConfigManager().getConfigCenters();
        if (CollectionUtils.isEmpty(configCenters)) {
            ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
            configCenterConfig.setScopeModel(applicationDeployer.getApplicationModel());
            configCenterConfig.refresh();
            ConfigValidationUtils.validateConfigCenterConfig(configCenterConfig);
            if (configCenterConfig.isValid()) {
                applicationDeployer.getConfigManager().addConfigCenter(configCenterConfig);
                configCenters = applicationDeployer.getConfigManager().getConfigCenters();
            }
        } else {
            for (ConfigCenterConfig configCenterConfig : configCenters) {
                configCenterConfig.refresh();
                ConfigValidationUtils.validateConfigCenterConfig(configCenterConfig);
            }
        }

        if (CollectionUtils.isNotEmpty(configCenters)) {

            Environment environment = applicationDeployer.getApplicationModel().modelEnvironment();
            CompositeDynamicConfiguration compositeDynamicConfiguration = new CompositeDynamicConfiguration();

            for (ConfigCenterConfig configCenter : configCenters) {
                // Pass config from ConfigCenterBean to applicationDeployer.getEnvironment()

                environment.updateExternalConfigMap(configCenter.getExternalConfiguration());
                environment.updateAppExternalConfigMap(configCenter.getAppExternalConfiguration());

                // Fetch config from remote config center
                compositeDynamicConfiguration.addConfiguration(prepareEnvironment(configCenter,environment));
            }
            environment.setDynamicConfiguration(compositeDynamicConfiguration);
        }
    }

    private DynamicConfiguration prepareEnvironment(ConfigCenterConfig configCenter,Environment environment) {

        if (configCenter.isValid()) {
            if (!configCenter.checkOrUpdateInitialized(true)) {
                return null;
            }

            DynamicConfiguration dynamicConfiguration;
            try {
                dynamicConfiguration = getDynamicConfiguration(configCenter.toUrl());
            } catch (Exception e) {
                if (!configCenter.isCheck()) {
                    logger.warn(CONFIG_FAILED_INIT_CONFIG_CENTER, "", "", "The configuration center failed to initialize", e);
                    configCenter.setInitialized(false);
                    return null;
                } else {
                    throw new IllegalStateException(e);
                }
            }
            ApplicationModel applicationModel = applicationDeployer.getApplicationModel();

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
    private DynamicConfiguration getDynamicConfiguration(URL connectionUrl) {
        String protocol = connectionUrl.getProtocol();

        DynamicConfigurationFactory factory = ConfigurationUtils.getDynamicConfigurationFactory(applicationDeployer.getApplicationModel(), protocol);
        return factory.getDynamicConfiguration(connectionUrl);
    }

    /**
     * For compatibility purpose, use registry as the default config center when
     * there's no config center specified explicitly and
     * useAsConfigCenter of registryConfig is null or true
     */
    private void useRegistryAsConfigCenterIfNecessary() {
        // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
        if (applicationDeployer.getApplicationModel().modelEnvironment().getDynamicConfiguration().isPresent()) {
            return;
        }

        if (CollectionUtils.isNotEmpty(applicationDeployer.getConfigManager().getConfigCenters())) {
            return;
        }

        // load registry
        applicationDeployer.getConfigManager().loadConfigsOfTypeFromProps(RegistryConfig.class);

        List<RegistryConfig> defaultRegistries = applicationDeployer.getConfigManager().getDefaultRegistries();
        if (defaultRegistries.size() > 0) {
            defaultRegistries
                .stream()
                .filter(this::isUsedRegistryAsConfigCenter)
                .map(this::registryAsConfigCenter)
                .forEach(configCenter -> {
                    if (applicationDeployer.getConfigManager().getConfigCenter(configCenter.getId()).isPresent()) {
                        return;
                    }
                    applicationDeployer.getConfigManager().addConfigCenter(configCenter);
                    logger.info("use registry as config-center: " + configCenter);

                });
        }
    }

    private boolean isUsedRegistryAsConfigCenter(RegistryConfig registryConfig) {
        return applicationDeployer.isUsedRegistryAsCenter(registryConfig, registryConfig::getUseAsConfigCenter, "config",
            DynamicConfigurationFactory.class);
    }

    private ConfigCenterConfig registryAsConfigCenter(RegistryConfig registryConfig) {

        String protocol = registryConfig.getProtocol();
        Integer port = registryConfig.getPort();
        URL url = URL.valueOf(registryConfig.getAddress(), registryConfig.getScopeModel());
        String id = "config-center-" + protocol + "-" + url.getHost() + "-" + port;

        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
        configCenterConfig.setId(id);
        configCenterConfig.setScopeModel(applicationDeployer.getApplicationModel());

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

        configCenterConfig.setAddress(applicationDeployer.getRegistryCompatibleAddress(registryConfig));
        configCenterConfig.setNamespace(registryConfig.getGroup());
        configCenterConfig.setUsername(registryConfig.getUsername());
        configCenterConfig.setPassword(registryConfig.getPassword());

        if (registryConfig.getTimeout() != null) {
            configCenterConfig.setTimeout(registryConfig.getTimeout().longValue());
        }
        configCenterConfig.setHighestPriority(false);
        return configCenterConfig;
    }


    /**
     * Supports the extension with the specified class and name
     *
     * @param extensionClass the {@link Class} of extension
     * @param name           the name of extension
     * @return if supports, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    private boolean supportsExtension(Class<?> extensionClass, String name) {
        if (isNotEmpty(name)) {
            ExtensionLoader<?> extensionLoader = applicationDeployer.getExtensionLoader(extensionClass);
            return extensionLoader.hasExtension(name);
        }
        return false;
    }

}
