package org.apache.dubbo.metrics.deploy;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;

import java.util.concurrent.atomic.AtomicBoolean;

@Activate(order = -1)
public class MetrisExportApplicationLifecycle implements ApplicationLifecycle {

    private DefaultApplicationDeployer applicationDeployer;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetrisExportApplicationLifecycle.class);

    @Override
    public void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer) {
         this.applicationDeployer= defaultApplicationDeployer;
    }

    /**
     * If this lifecycle need to initialize.
     */
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
        if(!hasPreparedApplicationInstance.get()) {
            exportMetricsService();
        }
    }

    private void exportMetricsService() {

        boolean exportMetrics = applicationDeployer.getApplicationModel().getApplicationConfigManager().getMetrics()
            .map(MetricsConfig::getExportMetricsService).orElse(true);

        if (exportMetrics) {
            try {

                MetricsApplicationLifecycle metricsApplicationLifecycle = applicationDeployer.getApplicationModel().getBeanFactory().getBean(MetricsApplicationLifecycle.class);
                MetricsServiceExporter metricsServiceExporter = null;

                if(metricsApplicationLifecycle != null){
                    metricsServiceExporter = metricsApplicationLifecycle.getMetricsServiceExporter();
                }
                if(metricsApplicationLifecycle != null) {
                    metricsServiceExporter.export();
                }
            } catch (Exception e) {
                logger.error(LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION, "", "",
                    "exportMetricsService an exception occurred when handle starting event", e);
            }
        }
    }
}
