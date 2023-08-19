package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.util.ConfigValidationUtils;


@Activate
public class MonitorConfigValidator implements ConfigValidator<MonitorConfig> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MonitorConfigValidator.class);

    public static void validateMonitorConfig(MonitorConfig config) {
        if (config != null) {
            if (!config.isValid()) {
                logger.info("There's no valid monitor config found, if you want to open monitor statistics for Dubbo, " +
                    "please make sure your monitor is configured properly.");
            }
            ConfigValidationUtils.checkParameterName(config.getParameters());
        }
    }

    @Override
    public void validate(MonitorConfig config) {
        validateMonitorConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return MonitorConfig.class.isAssignableFrom(configClass);
    }
}
