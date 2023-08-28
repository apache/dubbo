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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.config.deploy.lifecycle.manager.ApplicationLifecycleManager;
import org.apache.dubbo.config.utils.CompositeReferenceCache;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_START_MODEL;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

/**
 * initialize and start application instance
 */
public class DefaultApplicationDeployer extends AbstractDeployer<ApplicationModel> implements ApplicationDeployer {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultApplicationDeployer.class);
    private final ApplicationModel applicationModel;
    private final ConfigManager configManager;
    private final Environment environment;
    private final ReferenceCache referenceCache;
    private final FrameworkExecutorRepository frameworkExecutorRepository;
    private final ExecutorRepository executorRepository;
    private final AtomicBoolean hasPreparedApplicationInstance = new AtomicBoolean(false);
    private final AtomicBoolean hasPreparedInternalModule = new AtomicBoolean(false);
    private ScheduledFuture<?> asyncMetadataFuture;
    private volatile CompletableFuture<Boolean> startFuture;
    private final DubboShutdownHook dubboShutdownHook;
    private final ApplicationLifecycleManager lifecycleManager;
    private volatile MetricsServiceExporter metricsServiceExporter;

    private final Object stateLock = new Object();
    private final Object startLock = new Object();
    private final Object destroyLock = new Object();
    private final Object internalModuleLock = new Object();

    public DefaultApplicationDeployer(ApplicationModel applicationModel) {
        super(applicationModel);
        this.applicationModel = applicationModel;
        configManager = applicationModel.getApplicationConfigManager();
        environment = applicationModel.modelEnvironment();
        referenceCache = new CompositeReferenceCache(applicationModel);
        frameworkExecutorRepository = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);
        executorRepository = ExecutorRepository.getInstance(applicationModel);
        dubboShutdownHook = new DubboShutdownHook(applicationModel);
        lifecycleManager = new ApplicationLifecycleManager(applicationModel);
        // load spi listener
        Set<ApplicationDeployListener> deployListeners = applicationModel.getExtensionLoader(ApplicationDeployListener.class)
                .getSupportedExtensionInstances();
        for (ApplicationDeployListener listener : deployListeners) {
            this.addDeployListener(listener);
        }
    }

    public static ApplicationDeployer get(ScopeModel moduleOrApplicationModel) {
        ApplicationModel applicationModel = ScopeModelUtil.getApplicationModel(moduleOrApplicationModel);
        ApplicationDeployer applicationDeployer = applicationModel.getDeployer();
        if (applicationDeployer == null) {
            applicationDeployer = applicationModel.getBeanFactory().getOrRegisterBean(DefaultApplicationDeployer.class);
        }
        return applicationDeployer;
    }

    @Override
    public ApplicationModel getApplicationModel(){
            return applicationModel;
    }

    private <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return applicationModel.getExtensionLoader(type);
    }

    private void unRegisterShutdownHook() {
        dubboShutdownHook.unregister();
    }

    /**
     * Close registration of instance for pure Consumer process by setting registerConsumer to 'false'
     * by default is true.
     */
    private boolean isRegisterConsumerInstance() {
        Boolean registerConsumer = getApplication().getRegisterConsumer();
        if (registerConsumer == null) {
            return true;
        }
        return Boolean.TRUE.equals(registerConsumer);
    }

    @Override
    public ReferenceCache getReferenceCache() {
        return referenceCache;
    }

    /**
     * Initialize
     */
    @Override
    public void initialize() {
        if (initialized.get()) {
            return;
        }
        // Ensure that the initialization is completed when concurrent calls
        synchronized (startLock) {
            if (initialized.get()) {
                return;
            }
            onInitialize();

            // register shutdown hook
            registerShutdownHook();

            runInitialize();

            initialized.set(true);

            if (logger.isInfoEnabled()) {
                logger.info(getIdentifier() + " has been initialized!");
            }
        }
    }

    private void registerShutdownHook() {
        dubboShutdownHook.register();
    }

    //happens-before configcenter load
    private void loadApplicationConfigs() {
        configManager.loadConfigs();
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
            ExtensionLoader<?> extensionLoader = getExtensionLoader(extensionClass);
            return extensionLoader.hasExtension(name);
        }
        return false;
    }

    /**
     * Start the bootstrap
     *
     * @return
     */
    @Override
    public Future start() {
        synchronized (startLock) {
            if (isStopping() || isStopped() || isFailed()) {
                throw new IllegalStateException(getIdentifier() + " is stopping or stopped, can not start again");
            }

            try {
                // maybe call start again after add new module, check if any new module
                boolean hasPendingModule = hasPendingModule();

                if (isStarting()) {
                    // currently, is starting, maybe both start by module and application
                    // if it has new modules, start them
                    if (hasPendingModule) {
                        startModules();
                    }
                    // if it is starting, reuse previous startFuture
                    return startFuture;
                }

                // if is started and no new module, just return
                if (isStarted() && !hasPendingModule) {
                    return CompletableFuture.completedFuture(false);
                }

                // pending -> starting : first start app
                // started -> starting : re-start app
                onStarting();

                initialize();

                doStart();
            } catch (Throwable e) {
                onFailed(getIdentifier() + " start failure", e);
                throw e;
            }

            return startFuture;
        }
    }

    private boolean hasPendingModule() {
        boolean found = false;
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            if (moduleModel.getDeployer().isPending()) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
    public Future getStartFuture() {
        return startFuture;
    }

    private void doStart() {
        startModules();
        runStart();
    }

    private void startModules() {
        // ensure init and start internal module first
        prepareInternalModule();

        // filter and start pending modules, ignore new module during starting, throw exception of module start
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            if (moduleModel.getDeployer().isPending()) {
                moduleModel.getDeployer().start();
            }
        }
    }

    @Override
    public void prepareApplicationInstance() {
        //Migrated to ApplicationPrepareLifecycle

//        if (hasPreparedApplicationInstance.get()) {
//            return;
//        }
//        if (isRegisterConsumerInstance()) {
//            exportMetadataService();
//        }
    }

    @Override
    public void prepareInternalModule() {
        if (hasPreparedInternalModule.get()) {
            return;
        }
        synchronized (internalModuleLock) {
            if (hasPreparedInternalModule.get()) {
                return;
            }
            // start internal module
            ModuleDeployer internalModuleDeployer = applicationModel.getInternalModule().getDeployer();
            if (!internalModuleDeployer.isStarted()) {
                Future future = internalModuleDeployer.start();
                // wait for internal module startup
                try {
                    future.get(5, TimeUnit.SECONDS);
                 hasPreparedInternalModule.set(true);
                } catch (Exception e) {
                    logger.warn(CONFIG_FAILED_START_MODEL, "", "", "wait for internal module startup failed: " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public boolean isBackground() {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            if (moduleModel.getDeployer().isBackground()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Get the instance of {@link DynamicConfiguration} by the specified connection {@link URL} of config-center
     *
     * @param connectionURL of config-center
     * @return non-null
     * @since 2.7.5
     */
    private DynamicConfiguration getDynamicConfiguration(URL connectionURL) {
        String protocol = connectionURL.getProtocol();

        DynamicConfigurationFactory factory = ConfigurationUtils.getDynamicConfigurationFactory(applicationModel, protocol);
        return factory.getDynamicConfiguration(connectionURL);
    }

    private AtomicBoolean registered = new AtomicBoolean(false);

    private final AtomicInteger instanceRefreshScheduleTimes = new AtomicInteger(0);

    /**
     * Indicate that how many threads are updating service
     */
    private final AtomicInteger serviceRefreshState = new AtomicInteger(0);

    @Override
    public void refreshServiceInstance() {
        runRefreshServiceInstance();
    }

    @Override
    public void increaseServiceRefreshCount() {
        serviceRefreshState.incrementAndGet();
    }

    @Override
    public void decreaseServiceRefreshCount() {
        serviceRefreshState.decrementAndGet();
    }

    @Override
    public void stop() {
        applicationModel.destroy();
    }

    @Override
    public void preDestroy() {
        synchronized (destroyLock) {
            if (isStopping() || isStopped()) {
                return;
            }
            onStopping();

            runPreDestroy();

            unRegisterShutdownHook();
        }
    }

    @Override
    public void postDestroy() {
        synchronized (destroyLock) {
            // expect application model is destroyed before here
            if (isStopped()) {
                return;
            }
            try {
                runPostDestroy();

                executeShutdownCallbacks();

                // TODO should we close unused protocol server which only used by this application?
                // protocol server will be closed on all applications of same framework are stopped currently, but no associate to application
                // see org.apache.dubbo.config.deploy.FrameworkModelCleaner#destroyProtocols
                // see org.apache.dubbo.config.bootstrap.DubboBootstrapMultiInstanceTest#testMultiProviderApplicationStopOneByOne

                // destroy all executor services
                destroyExecutorRepository();

                onStopped();
            } catch (Throwable ex) {
                String msg = getIdentifier() + " an error occurred while stopping application: " + ex.getMessage();
                onFailed(msg, ex);
            }
        }
    }

    private void executeShutdownCallbacks() {
        ShutdownHookCallbacks shutdownHookCallbacks = applicationModel.getBeanFactory().getBean(ShutdownHookCallbacks.class);
        shutdownHookCallbacks.callback();
    }

    @Override
    public void notifyModuleChanged(ModuleModel moduleModel, DeployState state) {
        checkState(moduleModel, state);

        // notify module state changed or module changed
        synchronized (stateLock) {
            stateLock.notifyAll();
        }
    }

    @Override
    public void checkState(ModuleModel moduleModel, DeployState moduleState) {
        synchronized (stateLock) {

            runPreModuleChanged(moduleModel,moduleState);

            DeployState oldState = getState();
            DeployState newState = calculateState();

            if(newState == DeployState.STARTED && oldState == DeployState.STARTING){
                setStarted();
                try {
                    runPostModuleChanged(moduleModel, moduleState, newState, oldState);
                } finally {
                    completeStartFuture(true);
                }
            }else {
                switch (newState) {
                    case STARTING:
                        onStarting();
                        break;
                    case STOPPING:
                        onStopping();
                        break;
                    case STOPPED:
                        onStopped();
                        break;
                    case FAILED:
                        Throwable error = null;
                        ModuleModel errorModule = null;
                        for (ModuleModel module : applicationModel.getModuleModels()) {
                            ModuleDeployer deployer = module.getDeployer();
                            if (deployer.isFailed() && deployer.getError() != null) {
                                error = deployer.getError();
                                errorModule = module;
                                break;
                            }
                        }
                        onFailed(getIdentifier() + " found failed module: " + errorModule.getDesc(), error);
                        break;
                    case PENDING:
                        // cannot change to pending from other state
                        // setPending();
                        break;
                }
                runPostModuleChanged(moduleModel, moduleState, newState, oldState);
            }
        }
    }

    private DeployState calculateState() {
        DeployState newState = DeployState.UNKNOWN;
        int pending = 0, starting = 0, started = 0, stopping = 0, stopped = 0, failed = 0;
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            ModuleDeployer deployer = moduleModel.getDeployer();
            if (deployer == null) {
                pending++;
            } else if (deployer.isPending()) {
                pending++;
            } else if (deployer.isStarting()) {
                starting++;
            } else if (deployer.isStarted()) {
                started++;
            } else if (deployer.isStopping()) {
                stopping++;
            } else if (deployer.isStopped()) {
                stopped++;
            } else if (deployer.isFailed()) {
                failed++;
            }
        }

        if (failed > 0) {
            newState = DeployState.FAILED;
        } else if (started > 0) {
            if (pending + starting + stopping + stopped == 0) {
                // all modules have been started
                newState = DeployState.STARTED;
            } else if (pending + starting > 0) {
                // some module is pending and some is started
                newState = DeployState.STARTING;
            } else if (stopping + stopped > 0) {
                newState = DeployState.STOPPING;
            }
        } else if (starting > 0) {
            // any module is starting
            newState = DeployState.STARTING;
        } else if (pending > 0) {
            if (starting + starting + stopping + stopped == 0) {
                // all modules have not starting or started
                newState = DeployState.PENDING;
            } else if (stopping + stopped > 0) {
                // some is pending and some is stopping or stopped
                newState = DeployState.STOPPING;
            }
        } else if (stopping > 0) {
            // some is stopping and some stopped
            newState = DeployState.STOPPING;
        } else if (stopped > 0) {
            // all modules are stopped
            newState = DeployState.STOPPED;
        }
        return newState;
    }

    private void onInitialize() {
        for (DeployListener<ApplicationModel> listener :listeners) {
            try {
                listener.onInitialize(applicationModel);
            } catch (Throwable e) {
                logger.error(CONFIG_FAILED_START_MODEL, "", "", getIdentifier() + " an exception occurred when handle initialize event", e);
            }
        }
    }

    private void onStarting() {
        // pending -> starting
        // started -> starting
        if (!(isPending() || isStarted())) {
            return;
        }
        setStarting();
        startFuture = new CompletableFuture();
        if (logger.isInfoEnabled()) {
            logger.info(getIdentifier() + " is starting.");
        }
    }

    private void onStarted() {
            // starting -> started
            if (!isStarting()) {
                return;
            }
            setStarted();
    }

    private void completeStartFuture(boolean success) {
        if (startFuture != null) {
            startFuture.complete(success);
        }
    }

    private void onStopping() {
        try {
            if (isStopping() || isStopped()) {
                return;
            }
            setStopping();
            if (logger.isInfoEnabled()) {
                logger.info(getIdentifier() + " is stopping.");
            }
        } finally {
            completeStartFuture(false);
        }
    }

    private void onStopped() {
        try {
            if (isStopped()) {
                return;
            }
            setStopped();
            if (logger.isInfoEnabled()) {
                logger.info(getIdentifier() + " has stopped.");
            }
        } finally {
            completeStartFuture(false);
        }
    }

    private void onFailed(String msg, Throwable ex) {
        try {
            setFailed(ex);
            logger.error(CONFIG_FAILED_START_MODEL, "", "", msg, ex);
        } finally {
            completeStartFuture(false);
        }
    }

    private void destroyExecutorRepository() {
        // shutdown export/refer executor
        executorRepository.shutdownServiceExportExecutor();
        executorRepository.shutdownServiceReferExecutor();
        ExecutorRepository.getInstance(applicationModel).destroyAll();
    }

    private ApplicationConfig getApplication() {
        return configManager.getApplicationOrElseThrow();
    }

    private ApplicationContext newContext(){
        return new ApplicationContext(
            applicationModel,
            hasPreparedApplicationInstance,
            registered,
            hasPreparedInternalModule,
            referenceCache,
            serviceRefreshState,
            lifecycleManager,
            executorRepository,
            frameworkExecutorRepository,
            environment,
            applicationModel,
            getStateRef(),
            initialized,
            lastError,
            listeners
        );
    }

    private void runStart(){
        lifecycleManager.start(newContext());
    }

    private void runInitialize(){
        lifecycleManager.initialize(newContext());
    }

    private void runPreDestroy(){
        lifecycleManager.preDestroy(newContext());
    }

    private void runPostDestroy(){
        lifecycleManager.postDestroy(newContext());
    }

    private void runRefreshServiceInstance(){
        lifecycleManager.runRefreshServiceInstance(newContext());
    }

    private void runPreModuleChanged(ModuleModel changedModule, DeployState newState){
        lifecycleManager.preModuleChanged(newContext(),changedModule, newState);
    }

    private void runPostModuleChanged(ModuleModel changedModule,DeployState moduleNewState,DeployState applicationNewState,DeployState applicationOldState){
        lifecycleManager.postModuleChanged(newContext(),changedModule, moduleNewState, applicationNewState, applicationOldState);
    }

    @Override
    public void addDeployListener(DeployListener<ApplicationModel> listener) {
        applicationModel.getBeanFactory().registerBean(listener);
        super.addDeployListener(listener);
    }
}
