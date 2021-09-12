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
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.utils.ReferenceCache;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Export/refer services of module
 */
public class ModuleDeployer {

    private static final Logger logger = LoggerFactory.getLogger(ModuleDeployer.class);

    private final List<CompletableFuture<?>> asyncExportingFutures = new ArrayList<>();

    private final List<CompletableFuture<?>> asyncReferringFutures = new ArrayList<>();

    private List<ServiceConfigBase<?>> exportedServices = new ArrayList<>();

    private boolean asyncExportFinish = true;

    private volatile boolean asyncReferFinish = true;

    private ModuleModel moduleModel;

    private ExecutorRepository executorRepository;

    private final ModuleConfigManager configManager;

    private final SimpleReferenceCache referenceCache;
    private String identifier;

    private volatile boolean startup;

    protected AtomicBoolean initialized = new AtomicBoolean(false);

    private ApplicationDeployer applicationDeployer;

    public static ModuleDeployer get(ModuleModel moduleModel) {
        // create on first access
        return moduleModel.getBeanFactory().getOrRegisterBean(ModuleDeployer.class);
    }

    public ModuleDeployer(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
        configManager = moduleModel.getConfigManager();
        executorRepository = moduleModel.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
        referenceCache = SimpleReferenceCache.newCache();
        applicationDeployer = ApplicationDeployer.get(moduleModel);
    }

    public void initialize() throws IllegalStateException {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        loadConfigs();
    }

    public CompletableFuture start() throws IllegalStateException {

        CompletableFuture startFuture = new CompletableFuture();

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
            awaitFinish();
            onModuleStarted(startFuture);
        });
        return startFuture;
    }

    private boolean hasExportedServices() {
        return configManager.getServices().size() > 0;
    }

    public void destroy() throws IllegalStateException {
        unexportServices();
        unreferServices();
        onModuleStopped();
    }

    private void onModuleStopped() {
        startup = false;
        logger.info(getIdentifier() + " has stopped.");
        Set<ModuleDeployListener> listeners = moduleModel.getExtensionLoader(ModuleDeployListener.class).getSupportedExtensionInstances();
        for (ModuleDeployListener listener : listeners) {
            listener.onModuleStopped(moduleModel);
        }
    }

    private void onModuleStarted(CompletableFuture startFuture) {
        startup = true;
        logger.info(getIdentifier() + " has started.");
        startFuture.complete(true);
        Set<ModuleDeployListener> listeners = moduleModel.getExtensionLoader(ModuleDeployListener.class).getSupportedExtensionInstances();
        for (ModuleDeployListener listener : listeners) {
            listener.onModuleStarted(moduleModel);
        }
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
        if (sc.shouldExportAsync()) {
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
                // TODO, compatible with  ReferenceConfig.refer()
                ReferenceConfig<?> referenceConfig = (ReferenceConfig<?>) rc;
                if (!referenceConfig.isRefreshed()) {
                    referenceConfig.refresh();
                }

                if (rc.shouldInit()) {
                    if (rc.shouldReferAsync()) {
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
            asyncExportFinish = false;
            logger.info(getIdentifier() + " waiting services exporting ...");
            CompletableFuture<?> future = CompletableFuture.allOf(asyncExportingFutures.toArray(new CompletableFuture[0]));
            future.get();
        } catch (Exception e) {
            logger.warn(getIdentifier() + " export services occurred an exception.");
        } finally {
            executorRepository.shutdownServiceExportExecutor();
            logger.info(getIdentifier() + " export services finished.");
            asyncExportFinish = true;
            asyncExportingFutures.clear();
        }
    }

    private void waitReferFinish() {
        try {
            asyncReferFinish = false;
            logger.info(getIdentifier() + " waiting services referring ...");
            CompletableFuture<?> future = CompletableFuture.allOf(asyncReferringFutures.toArray(new CompletableFuture[0]));
            future.get();
        } catch (Exception e) {
            logger.warn(getIdentifier() + " refer services occurred an exception.");
        } finally {
            executorRepository.shutdownServiceReferExecutor();
            logger.info(getIdentifier() + " refer services finished.");
            asyncReferFinish = true;
            asyncReferringFutures.clear();
        }
    }

    public void awaitFinish() {
        waitExportFinish();
        waitReferFinish();
    }

    public boolean isExportBackground() {
        return moduleModel.getConfigManager().getProviders()
            .stream()
            .map(ProviderConfig::getExportBackground)
            .filter(k -> k != null && k)
            .findAny()
            .isPresent();
    }

    public boolean isReferBackground() {
        return moduleModel.getConfigManager().getConsumers()
            .stream()
            .map(ConsumerConfig::getReferBackground)
            .filter(k -> k != null && k)
            .findAny()
            .isPresent();
    }

    private String getIdentifier() {
        if (identifier == null) {
            identifier = moduleModel.getInternalName();
        }
        return identifier;
    }

    public ReferenceCache getReferenceCache() {
        return referenceCache;
    }

    /**
     * Prepare for export/refer service, trigger initializing application and module
     */
    public void prepare() {
        //TODO
        applicationDeployer.initialize();
        this.initialize();
    }

    /**
     * After export one service, trigger starting application
     */
    public void afterExportService(ServiceConfig sc) {
        //TODO
        applicationDeployer.prepareApplicationInstance();
    }

}
