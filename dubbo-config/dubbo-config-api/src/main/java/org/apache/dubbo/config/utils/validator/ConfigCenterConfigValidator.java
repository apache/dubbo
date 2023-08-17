package org.apache.dubbo.config.utils.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;

@Activate
public class ConfigCenterConfigValidator implements ConfigValidator<ConfigCenterConfig> {

    public static void validateConfigCenterConfig(ConfigCenterConfig config) {
        if (config != null) {
            ConfigValidationUtils.checkParameterName(config.getParameters());
        }
    }

    @Override
    public void validate(ConfigCenterConfig config) {
        validateConfigCenterConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ConfigCenterConfig.class.isAssignableFrom(configClass);
    }
}
