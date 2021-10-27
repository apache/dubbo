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
package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.common.deploy.AbstractDeployer;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.ModuleDeployListener;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Export/refer services of module
 */
public class DefaultModuleDeployer extends AbstractDeployer<ModuleModel> implements ModuleDeployer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultModuleDeployer.class);

    private final List<CompletableFuture<?>> asyncExportingFutures = new ArrayList<>();

    private final List<CompletableFuture<?>> asyncReferringFutures = new ArrayList<>();

    private List<ServiceConfigBase<?>> exportedServices = new ArrayList<>();

    private ModuleModel moduleModel;

    private ExecutorRepository executorRepository;

    private final ModuleConfigManager configManager;

    private final SimpleReferenceCache referenceCache;
    private String identifier;

    private ApplicationDeployer applicationDeployer;
    private CompletableFuture startFuture;
    private Boolean background;
    private Boolean exportAsync;
    private Boolean referAsync;


    public DefaultModuleDeployer(ModuleModel moduleModel) {
        super(moduleModel);
        this.moduleModel = moduleModel;
        configManager = moduleModel.getConfigManager();
        executorRepository = moduleModel.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
        referenceCache = SimpleReferenceCache.newCache();
        applicationDeployer = DefaultApplicationDeployer.get(moduleModel);

        //load spi listener
        Set<ModuleDeployListener> listeners = moduleModel.getExtensionLoader(ModuleDeployListener.class).getSupportedExtensionInstances();
        for (ModuleDeployListener listener : listeners) {
            this.addDeployListener(listener);
        }
    }

    @Override
    public void initialize() throws IllegalStateException {
        if (initialized.get()) {
            return;
        }
        // Ensure that the initialization is completed when concurrent calls
        synchronized (this) {
            if (initialized.get()) {
                return;
            }
            loadConfigs();

            // read ModuleConfig
            ModuleConfig moduleConfig = moduleModel.getConfigManager().getModule().orElseThrow(() -> new IllegalStateException("Default module config is not initialized"));
            exportAsync = Boolean.TRUE.equals(moduleConfig.getExportAsync());
            referAsync = Boolean.TRUE.equals(moduleConfig.getReferAsync());

            // start in background
            background = moduleConfig.getBackground();
            if (background == null) {
                // compatible with old usages
                background = isExportBackground() || isReferBackground();
            }

            initialized.set(true);
            if (logger.isInfoEnabled()) {
                logger.info(getIdentifier() + " has been initialized!");
            }
        }
    }

    @Override
    public synchronized Future start() throws IllegalStateException {
        if (isStarting() || isStarted()) {
            return startFuture;
        }

        onModuleStarting();
        startFuture = new CompletableFuture();

        applicationDeployer.initialize();

        // initialize
        initialize();

        // export services
        exportServices();

        // prepare application instance
        if (hasExportedServices()) {
            applicationDeployer.prepareApplicationInstance();
        }

        // refer services
        referServices();

        executorRepository.getSharedExecutor().submit(() -> {

            // wait for export finish
            waitExportFinish();

            // wait for refer finish
            waitReferFinish();

            onModuleStarted(startFuture);
        });

        return startFuture;
    }

    private boolean hasExportedServices() {
        return configManager.getServices().size() > 0;
    }

    @Override
    public void stop() throws IllegalStateException {
        moduleModel.destroy();
    }

    @Override
    public void preDestroy() throws IllegalStateException {
        if (isStopping() || isStopped()) {
            return;
        }
        onModuleStopping();
    }

    @Override
    public synchronized void postDestroy() throws IllegalStateException {
        if (isStopped()) {
            return;
        }
        unexportServices();
        unreferServices();

        ModuleServiceRepository serviceRepository = moduleModel.getServiceRepository();
        if (serviceRepository != null) {
            List<ConsumerModel> consumerModels = serviceRepository.getReferredServices();

            for (ConsumerModel consumerModel : consumerModels) {
                try {
                    if (consumerModel.getReferenceConfig() != null) {
                        consumerModel.getReferenceConfig().destroy();
                    } else if (consumerModel.getDestroyCaller() != null) {
                        consumerModel.getDestroyCaller().call();
                    }
                } catch (Throwable t) {
                    logger.error("Unable to destroy consumerModel.", t);
                }
            }

            List<ProviderModel> exportedServices = serviceRepository.getExportedServices();
            for (ProviderModel providerModel : exportedServices) {
                try {
                    if (providerModel.getServiceConfig() != null) {
                        providerModel.getServiceConfig().unexport();
                    } else if (providerModel.getDestroyCaller() != null) {
                        providerModel.getDestroyCaller().call();
                    }
                } catch (Throwable t) {
                    logger.error("Unable to destroy providerModel.", t);
                }
            }
            serviceRepository.destroy();
        }
        onModuleStopped();
    }

    private void onModuleStarting() {
        setStarting();
        logger.info(getIdentifier() + " is starting.");
        applicationDeployer.checkStarting();
    }

    private void onModuleStarted(CompletableFuture startFuture) {
        setStarted();
        logger.info(getIdentifier() + " has started.");
        applicationDeployer.checkStarted();
        // complete module start future after application state changed, fix #9012 ?
        startFuture.complete(true);
    }

    private void onModuleStopping() {
        setStopping();
        logger.info(getIdentifier() + " is stopping.");
    }

    private void onModuleStopped() {
        setStopped();
        logger.info(getIdentifier() + " has stopped.");
    }

    private void loadConfigs() {
        // load module configs
        moduleModel.getConfigManager().loadConfigs();
        moduleModel.getConfigManager().refreshAll();
    }

    private void exportServices() {
        for (ServiceConfigBase sc : configManager.getServices()) {
            exportServiceInternal(sc);
        }
    }

    private void exportServiceInternal(ServiceConfigBase sc) {
        ServiceConfig<?> serviceConfig = (ServiceConfig<?>) sc;
        if (!serviceConfig.isRefreshed()) {
            serviceConfig.refresh();
        }
        if (sc.isExported()) {
            return;
        }
        if (exportAsync || sc.shouldExportAsync()) {
            ExecutorService executor = executorRepository.getServiceExportExecutor();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    if (!sc.isExported()) {
                        sc.exportOnly();
                        exportedServices.add(sc);
                    }
                } catch (Throwable t) {
                    logger.error(getIdentifier() + " export async catch error : " + t.getMessage(), t);
                }
            }, executor);

            asyncExportingFutures.add(future);
        } else {
            if (!sc.isExported()) {
                sc.exportOnly();
                exportedServices.add(sc);
            }
        }
    }

    private void unexportServices() {
        exportedServices.forEach(sc -> {
            try {
                configManager.removeConfig(sc);
                sc.unexport();
            } catch (Exception ignored) {
                // ignored
            }
        });
        exportedServices.clear();

        asyncExportingFutures.forEach(future -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
        });
        asyncExportingFutures.clear();
    }

    private void referServices() {
        configManager.getReferences().forEach(rc -> {
            try {
                ReferenceConfig<?> referenceConfig = (ReferenceConfig<?>) rc;
                if (!referenceConfig.isRefreshed()) {
                    referenceConfig.refresh();
                }

                if (rc.shouldInit()) {
                    if (referAsync || rc.shouldReferAsync()) {
                        ExecutorService executor = executorRepository.getServiceReferExecutor();
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                referenceCache.get(rc);
                            } catch (Throwable t) {
                                logger.error(getIdentifier() + " refer async catch error : " + t.getMessage(), t);
                            }
                        }, executor);

                        asyncReferringFutures.add(future);
                    } else {
                        referenceCache.get(rc);
                    }
                }
            } catch (Throwable t) {
                logger.error(getIdentifier() + " refer catch error", t);
                referenceCache.destroy(rc);
            }
        });
    }

    private void unreferServices() {
        try {
            asyncReferringFutures.forEach(future -> {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            });
            asyncReferringFutures.clear();
            referenceCache.destroyAll();
        } catch (Exception ignored) {
        }
    }

    private void waitExportFinish() {
        try {
            logger.info(getIdentifier() + " waiting services exporting ...");
            CompletableFuture<?> future = CompletableFuture.allOf(asyncExportingFutures.toArray(new CompletableFuture[0]));
            future.get();
        } catch (Exception e) {
            logger.warn(getIdentifier() + " export services occurred an exception.");
        } finally {
            logger.info(getIdentifier() + " export services finished.");
            asyncExportingFutures.clear();
        }
    }

    private void waitReferFinish() {
        try {
            logger.info(getIdentifier() + " waiting services referring ...");
            CompletableFuture<?> future = CompletableFuture.allOf(asyncReferringFutures.toArray(new CompletableFuture[0]));
            future.get();
        } catch (Exception e) {
            logger.warn(getIdentifier() + " refer services occurred an exception.");
        } finally {
            logger.info(getIdentifier() + " refer services finished.");
            asyncReferringFutures.clear();
        }
    }

    @Override
    public boolean isBackground() {
        return background;
    }

    private boolean isExportBackground() {
        return moduleModel.getConfigManager().getProviders()
            .stream()
            .map(ProviderConfig::getExportBackground)
            .filter(k -> k != null && k)
            .findAny()
            .isPresent();
    }

    private boolean isReferBackground() {
        return moduleModel.getConfigManager().getConsumers()
            .stream()
            .map(ConsumerConfig::getReferBackground)
            .filter(k -> k != null && k)
            .findAny()
            .isPresent();
    }

    private String getIdentifier() {
        if (identifier == null) {
            identifier = "Dubbo module[" + moduleModel.getInternalId() + "]";
            if (moduleModel.getModelName() != null
                && !StringUtils.isEquals(moduleModel.getModelName(), moduleModel.getInternalName())) {
                identifier += "(" + moduleModel.getModelName() + ")";
            }
        }
        return identifier;
    }

    @Override
    public ReferenceCache getReferenceCache() {
        return referenceCache;
    }

    /**
     * Prepare for export/refer service, trigger initializing application and module
     */
    @Override
    public void prepare() {
        applicationDeployer.initialize();
        this.initialize();
    }

    /**
     * After export one service, trigger starting application
     */
    @Override
    public void notifyExportService(ServiceConfigBase<?> sc) {
        applicationDeployer.prepareApplicationInstance();
    }

}
