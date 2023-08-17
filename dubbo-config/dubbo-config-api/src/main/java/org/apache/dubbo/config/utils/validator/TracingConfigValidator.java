package org.apache.dubbo.config.utils.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.context.ConfigValidator;

@Activate
public class TracingConfigValidator implements ConfigValidator<TracingConfig> {

    public static void validateTracingConfig(TracingConfig tracingConfig) {
        // TODO
        if (tracingConfig == null) {
        }
    }

    @Override
    public void validate(TracingConfig config) {
        validateTracingConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return false;
    }
}
