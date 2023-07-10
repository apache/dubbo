package org.apache.dubbo.registry;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.DefaultModuleDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ModuleLifecycleManager;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Collections;
import java.util.List;

/**
 * Registry module lifecycle manager.
 */
public class RegistryModuleLifeManager implements ModuleLifecycleManager {

    private static final String NAME = "registry";

    private DefaultModuleDeployer moduleDeployer;

    private ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RegistryModuleLifeManager.class);

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean needInitialize() {
        return true;
    }

    public void setDeployer(DefaultModuleDeployer moduleDeployer) {
        this.moduleDeployer = moduleDeployer;
    }

    @Override
    public List<String> dependOnModulePreDestroy() {
        return Collections.singletonList("default");
    }

    @Override
    public void onModulePreDestroy() {
        offline();
    }

    private void offline() {
        try {
            ModuleServiceRepository serviceRepository = moduleDeployer.getModuleModel().getServiceRepository();
            List<ProviderModel> exportedServices = serviceRepository.getExportedServices();
            for (ProviderModel exportedService : exportedServices) {
                List<ProviderModel.RegisterStatedURL> statedUrls = exportedService.getStatedUrl();
                for (ProviderModel.RegisterStatedURL statedURL : statedUrls) {
                    if (statedURL.isRegistered()) {
                        doOffline(statedURL);
                    }
                }
            }
        } catch (Throwable t) {
            logger.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Exceptions occurred when unregister services.", t);
        }
    }

    private void doOffline(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory =
            statedURL.getRegistryUrl().getOrDefaultApplicationModel().getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.unregister(statedURL.getProviderUrl());
        statedURL.setRegistered(false);
    }

}
