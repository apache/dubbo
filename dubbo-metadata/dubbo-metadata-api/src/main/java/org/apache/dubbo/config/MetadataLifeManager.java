package org.apache.dubbo.config;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.*; //TODO: remove *
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_START_MODEL;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

/**
 * metadata package life manager
 */
public class MetadataLifeManager implements PackageLifeCycleManager {

    private static final String NAME = "metadata";

    private DefaultApplicationDeployer applicationDeployer;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetadataLifeManager.class);

    @Override
    public void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.applicationDeployer = defaultApplicationDeployer;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public List<String> dependOnInit() {
        return Collections.singletonList("metrics");
    }

    @Override
    public void initialize() {
        startMetadataCenter();
    }

    private void startMetadataCenter() {

        useRegistryAsMetadataCenterIfNecessary();

        ApplicationConfig applicationConfig = applicationDeployer.getApplication();
        ConfigManager configManager = applicationDeployer.getConfigManager();

        String metadataType = applicationConfig.getMetadataType();

        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                throw new IllegalStateException("No MetadataConfig found, Metadata Center address is required when 'metadata=remote' is enabled.");
            }
            return;
        }

        MetadataReportInstance metadataReportInstance = applicationDeployer.getApplicationModel().getBeanFactory().getBean(MetadataReportInstance.class);

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

    private void useRegistryAsMetadataCenterIfNecessary() {

        ConfigManager configManager = applicationDeployer.getConfigManager();

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
                .filter(this::isUsedRegistryAsMetadataCenter)
                .map(registryConfig -> registryAsMetadataCenter(registryConfig, metadataConfigToOverride))
                .forEach(metadataReportConfig -> {
                    overrideMetadataReportConfig(metadataConfigToOverride, metadataReportConfig);
                });
        }
    }

    private void overrideMetadataReportConfig(MetadataReportConfig metadataConfigToOverride, MetadataReportConfig metadataReportConfig) {

        ConfigManager configManager = applicationDeployer.getConfigManager();

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


    private boolean isUsedRegistryAsMetadataCenter(RegistryConfig registryConfig) {
        return applicationDeployer.isUsedRegistryAsCenter(registryConfig, registryConfig::getUseAsMetadataCenter, "metadata",
            MetadataReportFactory.class);
    }

    private MetadataReportConfig registryAsMetadataCenter(RegistryConfig registryConfig, MetadataReportConfig originMetadataReportConfig) {

        MetadataReportConfig metadataReportConfig = originMetadataReportConfig == null ?
            new MetadataReportConfig(registryConfig.getApplicationModel()) : originMetadataReportConfig;

        if (metadataReportConfig.getId() == null) {
            metadataReportConfig.setId(registryConfig.getId());
        }
        metadataReportConfig.setScopeModel(applicationDeployer.getApplicationModel());

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
            metadataReportConfig.setAddress(applicationDeployer.getRegistryCompatibleAddress(registryConfig));
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

    @Override
    public List<String> dependOnPreDestroy() {
        return Collections.singletonList("metrics");
    }

    @Override
    public void preDestroy() {
        if (applicationDeployer.getAsyncMetadataFuture() != null) {
            applicationDeployer.getAsyncMetadataFuture().cancel(true);
        }
    }

    @Override
    public List<String> dependOnPostDestroy() {
        return Collections.singletonList("registry");
    }

    @Override
    public void postDestroy() {
        destroyMetadataReports();
    }

    private void destroyMetadataReports() {
        // only destroy MetadataReport of this application
        List<MetadataReportFactory> metadataReportFactories = applicationDeployer.getExtensionLoader(MetadataReportFactory.class).getLoadedExtensionInstances();

        for (MetadataReportFactory metadataReportFactory : metadataReportFactories) {
            metadataReportFactory.destroy();
        }
    }

    @Override
    public List<String> dependOnModuleChanged() {
        return Collections.singletonList("metrics");
    }

    @Override
    public void moduleChanged(ModuleModel changedModule, DeployState moduleState) {
        exportMetadataService();
    }

    private void exportMetadataService() {
        if (!applicationDeployer.isStarting()) {
            return;
        }
        for (DeployListener<ApplicationModel> listener : applicationDeployer.getListeners()) {
            try {
                if (listener instanceof ApplicationDeployListener) {
                    ((ApplicationDeployListener) listener).onModuleStarted(applicationDeployer.getApplicationModel());
                }
            } catch (Throwable e) {
                logger.error(CONFIG_FAILED_START_MODEL, "", "", applicationDeployer.getIdentifier() + " an exception occurred when handle starting event", e);
            }
        }
    }


}
