package org.apache.dubbo.tracing.deploy;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycleManager;
import org.apache.dubbo.tracing.DubboObservationRegistry;
import org.apache.dubbo.tracing.utils.ObservationSupportUtil;

import java.util.Optional;

public class TracingApplicationLifecycleManger implements ApplicationLifecycleManager {

    private static final String NAME = "tracing";
    private DefaultApplicationDeployer defaultApplicationDeployer;
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(TracingApplicationLifecycleManger.class);

    @Override
    public void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.defaultApplicationDeployer = defaultApplicationDeployer;
    }


    @Override
    public void initialize() {
        initObservationRegistry();
    }

    private void initObservationRegistry() {

        if (!ObservationSupportUtil.isSupportObservation()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not found micrometer-observation or plz check the version of micrometer-observation version if already introduced, need > 1.10.0");
            }
            return;
        }
        if (!ObservationSupportUtil.isSupportTracing()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not found micrometer-tracing dependency, skip init ObservationRegistry.");
            }
            return;
        }

        Optional<TracingConfig> configManager = defaultApplicationDeployer.getConfigManager().getTracing();
        boolean needInitialize = configManager.isPresent() && configManager.get().getEnabled();

        if (needInitialize) {
            DubboObservationRegistry dubboObservationRegistry = new DubboObservationRegistry(defaultApplicationDeployer.getApplicationModel(), configManager.get());
            dubboObservationRegistry.initObservationRegistry();
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean needInitialize() {
        return true;
    }
}
