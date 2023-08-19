package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigValidator;

@Activate
public class MetricsConfigValidator implements ConfigValidator<MetricsConfig> {

    public static void validateMetricsConfig(MetricsConfig metricsConfig) {
        //TODO
        if (metricsConfig == null) {
        }
    }

    @Override
    public void validate(MetricsConfig config) {
        validateMetricsConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return MetricsConfig.class.isAssignableFrom(configClass);
    }
}
