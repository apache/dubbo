package org.apache.dubbo.registry.deploy;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_REFRESH_INSTANCE_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_REGISTER_INSTANCE_ERROR;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;

/**
 * Registry Package Life Manager.
 */
@Activate
public class RegistryApplicationLifecycle implements ApplicationLifecycle {

    private static final String NAME = "registry";

    private final AtomicInteger instanceRefreshScheduleTimes = new AtomicInteger(0);

    private DefaultApplicationDeployer applicationDeployer;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RegistryApplicationLifecycle.class);



    private ScheduledFuture<?> asyncMetadataFuture;

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

    /**
     * {@link ApplicationDeployer#start()}
     *
     * @param hasPreparedApplicationInstance
     */
    @Override
    public void start(AtomicBoolean hasPreparedApplicationInstance) {
        if(!hasPreparedApplicationInstance.get() && hasPreparedApplicationInstance.compareAndSet(false, true)) {
            // register the local ServiceInstance if required
            registerServiceInstance();
        }
    }

    @Override
    public void preDestroy() {
        offline();
        unregisterMetadataServiceInstance();

        if (asyncMetadataFuture != null) {
            asyncMetadataFuture.cancel(true);
        }
    }

    private void offline() {
        try {
            for (ModuleModel moduleModel : applicationDeployer.getApplicationModel().getModuleModels()) {
                ModuleServiceRepository serviceRepository = moduleModel.getServiceRepository();
                List<ProviderModel> exportedServices = serviceRepository.getExportedServices();
                for (ProviderModel exportedService : exportedServices) {
                    List<ProviderModel.RegisterStatedURL> statedUrls = exportedService.getStatedUrl();
                    for (ProviderModel.RegisterStatedURL statedURL : statedUrls) {
                        if (statedURL.isRegistered()) {
                            doOffline(statedURL);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Exceptions occurred when unregister services.", t);
        }
    }

    private void unregisterMetadataServiceInstance() {
        if (applicationDeployer.isRegistered()) {
            ServiceInstanceMetadataUtils.unregisterMetadataAndInstance(applicationDeployer.getApplicationModel());
        }
    }

    private void doOffline(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory =
            statedURL.getRegistryUrl().getOrDefaultApplicationModel().getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.unregister(statedURL.getProviderUrl());
        statedURL.setRegistered(false);
    }

    /**
     * postDestroy.
     */
    @Override
    public void postDestroy() {
        destroyRegistries();
    }

    private void destroyRegistries() {
        RegistryManager.getInstance(applicationDeployer.getApplicationModel()).destroyAll();
    }


    @Override
    public List<String> dependOnPreModuleChanged() {
        return Collections.singletonList("metadata");
    }

    /**
     * What to do when a module changed.
     *
     * @param changedModule changedModule
     * @param moduleState   moduleState
     */
    @Override
    public void preModuleChanged(ModuleModel changedModule, DeployState moduleState, AtomicBoolean hasPreparedApplicationInstance) {

        if (!changedModule.isInternal() && moduleState == DeployState.STARTED &&
            !hasPreparedApplicationInstance.get() &&
            applicationDeployer.isRegisterConsumerInstance() &&
            hasPreparedApplicationInstance.compareAndSet(false,true)
        ) {
            registerServiceInstance();
        }
    }

    private void registerServiceInstance() {

        ApplicationModel applicationModel = applicationDeployer.getApplicationModel();
        FrameworkExecutorRepository frameworkExecutorRepository = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);

        try {
            applicationDeployer.setRegistered(true);
            MetricsEventBus.post(RegistryEvent.toRegisterEvent(applicationModel),
                () -> {
                    ServiceInstanceMetadataUtils.registerMetadataAndInstance(applicationModel);
                    return null;
                }
            );
        } catch (Exception e) {
            logger.error(CONFIG_REGISTER_INSTANCE_ERROR, "configuration server disconnected", "", "Register instance error.", e);
        }

        if (applicationDeployer.isRegistered()) {
            // scheduled task for updating Metadata and ServiceInstance
            asyncMetadataFuture = frameworkExecutorRepository.getSharedScheduledExecutor().scheduleWithFixedDelay(() -> {

                // ignore refresh metadata on stopping
                if (applicationModel.isDestroyed()) {
                    return;
                }

                // refresh for 30 times (default for 30s) when deployer is not started, prevent submit too many revision
                if (instanceRefreshScheduleTimes.incrementAndGet() % 30 != 0 && !applicationDeployer.isStarted()) {
                    return;
                }

                // refresh for 5 times (default for 5s) when services are being updated by other threads, prevent submit too many revision
                // note: should not always wait here
                if (applicationDeployer.getServiceRefreshState() != 0 && instanceRefreshScheduleTimes.get() % 5 != 0) {
                    return;
                }

                try {
                    if (!applicationModel.isDestroyed() && applicationDeployer.isRegistered()) {
                        ServiceInstanceMetadataUtils.refreshMetadataAndInstance(applicationModel);
                    }
                } catch (Exception e) {
                    if (!applicationModel.isDestroyed()) {
                        logger.error(CONFIG_REFRESH_INSTANCE_ERROR, "", "", "Refresh instance and metadata error.", e);
                    }
                }
            }, 0, ConfigurationUtils.get(applicationModel, METADATA_PUBLISH_DELAY_KEY, DEFAULT_METADATA_PUBLISH_DELAY), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public List<String> dependOnPostModuleChanged() {
        return Collections.singletonList("metrics");
    }

    @Override
    public void postModuleChanged(ModuleModel changedModule, DeployState moduleState, DeployState newState) {
        if(!applicationDeployer.isStarting() && newState.equals(DeployState.STARTED)){
            refreshMetadata();
        }
    }

    private void refreshMetadata(){
        try {
            if (applicationDeployer.isRegistered()) {
                ServiceInstanceMetadataUtils.refreshMetadataAndInstance(applicationDeployer.getApplicationModel());
            }
        } catch (Exception e) {
            logger.error(CONFIG_REFRESH_INSTANCE_ERROR, "", "", "Refresh instance and metadata error.", e);
        }
    }

}
