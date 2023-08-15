package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

@Activate(order = -700)
public class MetadataInitializeLifecycle implements ApplicationLifecycle{

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetadataInitializeLifecycle.class);

    /**
     * If this lifecycle need to initialize.
     */
    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    @Override
    public void initialize(ApplicationContext applicationContext) {
        startMetadataCenter(applicationContext);
    }

    private void startMetadataCenter(ApplicationContext applicationContext) {
        ApplicationModel applicationModel = applicationContext.getModel();
        useRegistryAsMetadataCenterIfNecessary(applicationModel);

        ApplicationConfig applicationConfig = applicationModel.getCurrentConfig();
        ConfigManager configManager = applicationModel.getApplicationConfigManager();

        String metadataType = applicationConfig.getMetadataType();

        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                throw new IllegalStateException("No MetadataConfig found, Metadata Center address is required when 'metadata=remote' is enabled.");
            }
            return;
        }

        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);

        List<MetadataReportConfig> validMetadataReportConfigs = new ArrayList<>(metadataReportConfigs.size());

        for (MetadataReportConfig metadataReportConfig : metadataReportConfigs) {
            if (ConfigValidationUtils.isValidMetadataConfig(metadataReportConfig)) {
                ConfigValidationUtils.validateMetadataConfig(metadataReportConfig);
                validMetadataReportConfigs.add(metadataReportConfig);
            }
        }
        metadataReportInstance.init(validMetadataReportConfigs);

        if (!metadataReportInstance.inited()) {
            throw new IllegalStateException(String.format("%s MetadataConfigs found, but none of them is valid.", metadataReportConfigs.size()));
        }
    }

    private void useRegistryAsMetadataCenterIfNecessary(ApplicationModel applicationModel) {

        ConfigManager configManager = applicationModel.getApplicationConfigManager();

        Collection<MetadataReportConfig> originMetadataConfigs = configManager.getMetadataConfigs();
        if (originMetadataConfigs.stream().anyMatch(m -> Objects.nonNull(m.getAddress()))) {
            return;
        }

        Collection<MetadataReportConfig> metadataConfigsToOverride = originMetadataConfigs
            .stream()
            .filter(m -> Objects.isNull(m.getAddress()))
            .collect(Collectors.toList());

        if (metadataConfigsToOverride.size() > 1) {
            return;
        }

        MetadataReportConfig metadataConfigToOverride = metadataConfigsToOverride.stream().findFirst().orElse(null);

        List<RegistryConfig> defaultRegistries = configManager.getDefaultRegistries();
        if (!defaultRegistries.isEmpty()) {
            defaultRegistries
                .stream()
                .filter(registry -> isUsedRegistryAsMetadataCenter(applicationModel,registry))
                .map(registryConfig -> registryAsMetadataCenter(applicationModel, registryConfig, metadataConfigToOverride))
                .forEach(metadataReportConfig -> {
                    overrideMetadataReportConfig(applicationModel, metadataConfigToOverride, metadataReportConfig);
                });
        }
    }

    private MetadataReportConfig registryAsMetadataCenter(ApplicationModel applicationModel,RegistryConfig registryConfig, MetadataReportConfig originMetadataReportConfig) {

        MetadataReportConfig metadataReportConfig = originMetadataReportConfig == null ?
            new MetadataReportConfig(registryConfig.getApplicationModel()) : originMetadataReportConfig;

        if (metadataReportConfig.getId() == null) {
            metadataReportConfig.setId(registryConfig.getId());
        }
        metadataReportConfig.setScopeModel(applicationModel);

        if (metadataReportConfig.getParameters() == null) {
            metadataReportConfig.setParameters(new HashMap<>());
        }
        if (CollectionUtils.isNotEmptyMap(registryConfig.getParameters())) {

            for (Map.Entry<String, String> entry : registryConfig.getParameters().entrySet()) {
                metadataReportConfig.getParameters().putIfAbsent(entry.getKey(), entry.getValue()); // copy the parameters
            }
        }
        metadataReportConfig.getParameters().put(CLIENT_KEY, registryConfig.getClient());

        if (metadataReportConfig.getGroup() == null) {
            metadataReportConfig.setGroup(registryConfig.getGroup());
        }
        if (metadataReportConfig.getAddress() == null) {
            metadataReportConfig.setAddress(getRegistryCompatibleAddress(registryConfig));
        }
        if (metadataReportConfig.getUsername() == null) {
            metadataReportConfig.setUsername(registryConfig.getUsername());
        }
        if (metadataReportConfig.getPassword() == null) {
            metadataReportConfig.setPassword(registryConfig.getPassword());
        }
        if (metadataReportConfig.getTimeout() == null) {
            metadataReportConfig.setTimeout(registryConfig.getTimeout());
        }
        return metadataReportConfig;
    }

    private boolean isUsedRegistryAsMetadataCenter(ApplicationModel applicationModel,RegistryConfig registryConfig) {
        return isUsedRegistryAsCenter(applicationModel,registryConfig, registryConfig::getUseAsMetadataCenter, "metadata",
            MetadataReportFactory.class);
    }

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


    private void overrideMetadataReportConfig(ApplicationModel applicationModel,MetadataReportConfig metadataConfigToOverride, MetadataReportConfig metadataReportConfig) {

        ConfigManager configManager = applicationModel.getApplicationConfigManager();

        if (metadataReportConfig.getId() == null) {
            Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
            if (CollectionUtils.isNotEmpty(metadataReportConfigs)) {
                for (MetadataReportConfig existedConfig : metadataReportConfigs) {
                    if (existedConfig.getId() == null && existedConfig.getAddress().equals(metadataReportConfig.getAddress())) {
                        return;
                    }
                }
            }
            configManager.removeConfig(metadataConfigToOverride);
            configManager.addMetadataReport(metadataReportConfig);
        } else {
            Optional<MetadataReportConfig> configOptional = configManager.getConfig(MetadataReportConfig.class, metadataReportConfig.getId());
            if (configOptional.isPresent()) {
                return;
            }
            configManager.removeConfig(metadataConfigToOverride);
            configManager.addMetadataReport(metadataReportConfig);
        }
        logger.info("use registry as metadata-center: " + metadataReportConfig);
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

}
