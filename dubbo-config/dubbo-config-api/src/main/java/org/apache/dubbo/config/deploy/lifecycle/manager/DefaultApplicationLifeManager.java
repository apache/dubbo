package org.apache.dubbo.config.deploy.lifecycle.manager;

import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycleManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_START_MODEL;

public class DefaultApplicationLifeManager implements ApplicationLifecycleManager {

    private static final String NAME = "default";

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultApplicationLifeManager.class);

    private DefaultApplicationDeployer applicationDeployer;

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
    public void initialize() {
        onInitialize();
    }

    private void onInitialize() {
        for (DeployListener<ApplicationModel> listener : applicationDeployer.getListeners()) {
            try {
                listener.onInitialize(applicationDeployer.getApplicationModel());
            } catch (Throwable e) {
                logger.error(CONFIG_FAILED_START_MODEL, "", "", applicationDeployer.getIdentifier() + " an exception occurred when handle initialize event", e);
            }
        }
    }

    @Override
    public void postModuleChanged(ModuleModel changedModule, DeployState moduleState, DeployState applicationNewState) {
        switch (applicationNewState) {
            case STARTED:
                onStarted();
                break;
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
                for (ModuleModel module : applicationDeployer.getApplicationModel().getModuleModels()) {
                    ModuleDeployer deployer = module.getDeployer();
                    if (deployer.isFailed() && deployer.getError() != null) {
                        error = deployer.getError();
                        errorModule = module;
                        break;
                    }
                }
                onFailed(applicationDeployer.getIdentifier() + " found failed module: " + errorModule.getDesc(), error);
                break;
            case PENDING:
                // cannot change to pending from other state
                // setPending();
                break;
        }

        applicationDeployer.setStarted();
    }

    private void onStarted() {
        // starting -> started
        if (!applicationDeployer.isStarting()) {
            return;
        }
        applicationDeployer.setStarted();
    }

    private void onStopping() {
        try {
            if (applicationDeployer.isStopping() || applicationDeployer.isStopped()) {
                return;
            }
            applicationDeployer.setStopping();
            if (logger.isInfoEnabled()) {
                logger.info(applicationDeployer.getIdentifier() + " is stopping.");
            }
        } finally {
            applicationDeployer.completeStartFuture(false);
        }
    }

    private void onStopped() {
        try {
            if (applicationDeployer.isStopped()) {
                return;
            }
            applicationDeployer.setStopped();
            if (logger.isInfoEnabled()) {
                logger.info(applicationDeployer.getIdentifier() + " has stopped.");
            }
        } finally {
            applicationDeployer.completeStartFuture(false);
        }
    }

    private void onFailed(String msg, Throwable ex) {
        try {
            applicationDeployer.setFailed(ex);
            logger.error(CONFIG_FAILED_START_MODEL, "", "", msg, ex);
        } finally {
            applicationDeployer.completeStartFuture(false);
        }
    }

    private void onStarting() {
        // pending -> starting
        // started -> starting
        if (!(applicationDeployer.isPending() || applicationDeployer.isStarted())) {
            return;
        }
        applicationDeployer.setStarting();
        applicationDeployer.setStartFuture(new CompletableFuture());
        if (logger.isInfoEnabled()) {
            logger.info(applicationDeployer.getIdentifier() + " is starting.");
        }
    }

    @Override
    public List<String> postDestroyDependencies() {
        return Arrays.asList("registry","metadata");
    }

    /**
     * postDestroy.
     */
    @Override
    public void postDestroy() {
       executeShutdownCallbacks();
    }

    private void executeShutdownCallbacks() {
        ShutdownHookCallbacks shutdownHookCallbacks = applicationDeployer.getApplicationModel().getBeanFactory().getBean(ShutdownHookCallbacks.class);
        shutdownHookCallbacks.callback();
    }

    private void destroyExecutorRepository() {
        // shutdown export/refer executor
        ExecutorRepository executorRepository = applicationDeployer.getExecutorRepository();
        executorRepository.shutdownServiceExportExecutor();
        executorRepository.shutdownServiceReferExecutor();
        ExecutorRepository.getInstance(applicationDeployer.getApplicationModel()).destroyAll();
    }

}
