package org.apache.dubbo.metrics.deploy;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.lifecycle.ApplicationLifecycle;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.report.DefaultMetricsReporterFactory;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.metrics.report.MetricsReporterFactory;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_DEFAULT;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

/**
 * Metrics lifecycle.
 */
public class MetricsApplicationLifecycle implements ApplicationLifecycle {

    private static final String NAME = "metrics";

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetricsApplicationLifecycle.class);

    private DefaultApplicationDeployer applicationDeployer;

    private MetricsServiceExporter metricsServiceExporter;

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
        return isSupportMetrics();
    }

    /**
     * {@link ApplicationDeployer#start()}
     */
    @Override
    public void start(AtomicBoolean hasPreparedApplicationInstance) {
        if(!hasPreparedApplicationInstance.get()) {
            exportMetricsService();
        }
    }

    @Override
    public List<String> dependOnInit() {
        return Collections.singletonList("config_center");
    }

    /**
     * Initialize.
     */
    @Override
    public void initialize() {
        initMetricsReporter();
        initMetricsService();
    }

    private void initMetricsReporter() {

        ApplicationModel applicationModel = applicationDeployer.getApplicationModel();
        DefaultMetricsCollector collector = applicationDeployer.getApplicationModel().getBeanFactory().getBean(DefaultMetricsCollector.class);
        Optional<MetricsConfig> configOptional = applicationDeployer.getApplicationModel().getApplicationConfigManager().getMetrics();;

        //If no specific metrics type is configured and there is no Prometheus dependency in the dependencies.
        MetricsConfig metricsConfig = configOptional.orElse(new MetricsConfig(applicationDeployer.getApplicationModel()));

        if (StringUtils.isBlank(metricsConfig.getProtocol())) {
            metricsConfig.setProtocol(isSupportPrometheus() ? PROTOCOL_PROMETHEUS : PROTOCOL_DEFAULT);
        }

        collector.setCollectEnabled(true);
        collector.collectApplication();
        collector.setThreadpoolCollectEnabled(Optional.ofNullable(metricsConfig.getEnableThreadpool()).orElse(true));

        MetricsReporterFactory metricsReporterFactory = applicationModel.getExtensionLoader(MetricsReporterFactory.class).getAdaptiveExtension();
        MetricsReporter metricsReporter;

        try {
            metricsReporter = metricsReporterFactory.createMetricsReporter(metricsConfig.toUrl());
        } catch (IllegalStateException e) {
            if (e.getMessage().startsWith("No such extension org.apache.dubbo.metrics.report.MetricsReporterFactory")) {
                logger.warn(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", e.getMessage());
                return;
            } else {
                throw e;
            }
        }
        metricsReporter.init();
        applicationModel.getBeanFactory().registerBean(metricsReporter);

        //If the protocol is not the default protocol, the default protocol is also initialized.
        if (!PROTOCOL_DEFAULT.equals(metricsConfig.getProtocol())) {
            DefaultMetricsReporterFactory defaultMetricsReporterFactory = new DefaultMetricsReporterFactory(applicationModel);
            MetricsReporter defaultMetricsReporter = defaultMetricsReporterFactory.createMetricsReporter(metricsConfig.toUrl());
            defaultMetricsReporter.init();
            applicationModel.getBeanFactory().registerBean(defaultMetricsReporter);
        }
    }

    private void initMetricsService() {
        this.metricsServiceExporter = applicationDeployer.getApplicationModel().getExtensionLoader(MetricsServiceExporter.class).getDefaultExtension();
        this.metricsServiceExporter.init();
    }

    public boolean isSupportMetrics() {
        return isClassPresent("io.micrometer.core.instrument.MeterRegistry");
    }

    public boolean isSupportPrometheus() {
        return isClassPresent("io.micrometer.prometheus.PrometheusConfig")
            && isClassPresent("io.prometheus.client.exporter.BasicAuthHttpConnectionFactory")
            && isClassPresent("io.prometheus.client.exporter.HttpConnectionFactory")
            && isClassPresent("io.prometheus.client.exporter.PushGateway");
    }

    private boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, DefaultApplicationDeployer.class.getClassLoader());
    }

    @Override
    public List<String> dependOnPreDestroy() {
        return Collections.singletonList("registry");
    }

    @Override
    public void preDestroy() {
        disableMetricsService();
    }

    private void disableMetricsService() {
        if (metricsServiceExporter != null) {
            try {
                metricsServiceExporter.unexport();
            } catch (Exception ignored) {
                // ignored
            }
        }
    }

    /**
     * What to do when a module changed.
     *
     * @param changedModule changedModule
     * @param moduleState   moduleState
     */
    @Override
    public void preModuleChanged(ModuleModel changedModule, DeployState moduleState, AtomicBoolean hasPreparedApplicationInstance) {
        if (!changedModule.isInternal() && moduleState == DeployState.STARTED && ! hasPreparedApplicationInstance.get()) {
            exportMetricsService();
        }
    }

    private void exportMetricsService() {
        boolean exportMetrics = applicationDeployer.getApplicationModel().getApplicationConfigManager().getMetrics()
            .map(MetricsConfig::getExportMetricsService).orElse(true);

        if (exportMetrics) {
            try {
                this.metricsServiceExporter.export();
            } catch (Exception e) {
                logger.error(LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION, "", "",
                    "exportMetricsService an exception occurred when handle starting event", e);
            }
        }
    }

    @Override
    public List<String> dependOnPostModuleChanged() {
        return Collections.singletonList("default");
    }

    @Override
    public void postModuleChanged(ModuleModel changedModule, DeployState moduleState, DeployState newState) {
        if(DeployState.STARTED.equals(newState) && applicationDeployer.isStarting()){
            startMetricsCollector();
        }
    }

    private void startMetricsCollector() {

        DefaultMetricsCollector collector = applicationDeployer.getApplicationModel().getBeanFactory().getBean(DefaultMetricsCollector.class);


        if (Objects.nonNull(collector) && collector.isThreadpoolCollectEnabled()) {
            collector.registryDefaultSample();
        }
        if (logger.isInfoEnabled()) {
            logger.info(applicationDeployer.getIdentifier() + " is ready.");
        }
    }

}
