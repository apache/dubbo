package org.apache.dubbo.config.metrics;

import static org.apache.dubbo.common.constants.CommonConstants.METRICS_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METRICS_SERVICE_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.metrics.service.MetricsService;
import org.apache.dubbo.common.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.builders.ExportServiceConfigBuilder;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

/**
 * Export metrics service
 */
public class DefaultMetricsServiceExporter implements MetricsServiceExporter, ScopeModelAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private          ApplicationModel              applicationModel;
    private          MetricsService                metricsService;
    private volatile ServiceConfig<MetricsService> serviceConfig;

    @Override
    public void init() {
        initialize();
    }

    private void initialize() {
        MetricsConfig metricsConfig = applicationModel.getApplicationConfigManager().getMetrics().orElse(null);
        // TODO compatible with old usage of metrics, remove protocol check after new metrics is ready for use.
        if (metricsConfig != null) {
            if (PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol())) {
                ExtensionLoader<MetricsService> extensionLoader = applicationModel.getExtensionLoader(MetricsService.class);
                if (!extensionLoader.hasExtension(MetricsService.DEFAULT_EXTENSION_NAME)) {
                    throw new IllegalStateException("Metrics config exist, but the dubbo-metrics-api dependency is missing. Please check your project dependencies.");
                } else {
                    this.metricsService = extensionLoader.getDefaultExtension();
                }
            } else {
                logger.warn("Protocol " + metricsConfig.getProtocol() + " not support for new metrics mechanism. " +
                    "Using old metrics mechanism instead.");
            }
        }
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public MetricsServiceExporter export() {
        if (metricsService != null) {
            if (!isExported()) {
                ServiceConfig<MetricsService> serviceConfig = ExportServiceConfigBuilder.<MetricsService>newServiceBuilder(applicationModel)
                    .interfaceClass(MetricsService.class)
                    .protocol(getApplicationConfig().getMetricsServiceProtocol(), METRICS_SERVICE_PROTOCOL_KEY)
                    .port(getApplicationConfig().getMetricsServicePort(), METRICS_SERVICE_PORT_KEY)
                    .ref(metricsService)
                    .registryId("internal-metrics-registry")
                    .build();

                // export
                serviceConfig.export();

                if (logger.isInfoEnabled()) {
                    logger.info("The MetricsService exports urls : " + serviceConfig.getExportedUrls());
                }
                this.serviceConfig = serviceConfig;
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("The MetricsService has been exported : " + serviceConfig.getExportedUrls());
                }
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("The MetricsConfig not exist, will not export metrics service.");
            }
        }

        return this;
    }

    @Override
    public MetricsServiceExporter unexport() {
        if (isExported()) {
            serviceConfig.unexport();
        }
        return this;
    }

    private ApplicationConfig getApplicationConfig() {
        return applicationModel.getApplicationConfigManager().getApplication().get();
    }

    private boolean isExported() {
        return serviceConfig != null && serviceConfig.isExported() && !serviceConfig.isUnexported();
    }

}
