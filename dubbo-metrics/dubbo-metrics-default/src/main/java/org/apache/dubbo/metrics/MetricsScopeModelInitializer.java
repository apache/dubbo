package org.apache.dubbo.metrics;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.event.GlobalMetricsEventMulticaster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

public class MetricsScopeModelInitializer implements ScopeModelInitializer {

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {

    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        MetricsConfig metricsConfig = configManager.getMetrics().orElse(null);
        if (metricsConfig != null && PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol())) {
            DefaultMetricsCollector collector = beanFactory.getOrRegisterBean(DefaultMetricsCollector.class);
            collector.setCollectEnabled(true);
            collector.collectApplication(applicationModel);
            beanFactory.registerBean(GlobalMetricsEventMulticaster.class);
        }

    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {

    }
}
