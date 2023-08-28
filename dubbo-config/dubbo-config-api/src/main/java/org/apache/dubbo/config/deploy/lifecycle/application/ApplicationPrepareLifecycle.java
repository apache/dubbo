package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_START_MODEL;

@Activate(order = -4000)
public class ApplicationPrepareLifecycle implements ApplicationLifecycle{

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ApplicationPrepareLifecycle.class);

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    @Override
    public void preModuleChanged(ApplicationContext applicationContext, ModuleModel changedModule, DeployState moduleState) {
        if (!changedModule.isInternal() && moduleState == DeployState.STARTED) {
            prepareApplicationInstance(applicationContext);
        }
    }

    public void prepareApplicationInstance(ApplicationContext applicationContext) {
        if (applicationContext.getHasPreparedApplicationInstance().get()) {
            return;
        }
        if (isRegisterConsumerInstance(applicationContext.getModel().getApplicationConfigManager().getApplicationOrElseThrow())) {
            notifyStartingListener(applicationContext);
        }
    }

    private boolean isRegisterConsumerInstance(ApplicationConfig applicationConfig) {
        Boolean registerConsumer = applicationConfig.getRegisterConsumer();
        if (registerConsumer == null) {
            return true;
        }
        return Boolean.TRUE.equals(registerConsumer);
    }

    private void notifyStartingListener(ApplicationContext applicationContext) {
        if (!applicationContext.getCurrentState().equals(DeployState.STARTING)) {
            return;
        }
        for (DeployListener<ApplicationModel> listener : applicationContext.getDeployListeners()) {
            try {
                if (listener instanceof ApplicationDeployListener) {
                    ((ApplicationDeployListener) listener).onModuleStarted(applicationContext.getModel());
                }
            } catch (Throwable e) {
                logger.error(CONFIG_FAILED_START_MODEL, "", "", applicationContext.getModel().getDesc() + " an exception occurred when handle starting event", e);
            }
        }
    }


}
