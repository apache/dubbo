package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.util.ConfigValidationUtils;

import static org.apache.dubbo.config.Constants.NAME;
import static org.apache.dubbo.config.Constants.ORGANIZATION;
import static org.apache.dubbo.config.Constants.OWNER;

@Activate
public class ModuleConfigValidator implements ConfigValidator<ModuleConfig> {

    @Override
    public void validate(ModuleConfig config) {
        validateModuleConfig(config);
    }

    public static void validateModuleConfig(ModuleConfig config) {
        if (config != null) {
            ConfigValidationUtils.checkName(NAME, config.getName());
            ConfigValidationUtils.checkName(OWNER, config.getOwner());
            ConfigValidationUtils.checkName(ORGANIZATION, config.getOrganization());
        }
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ModuleConfig.class.equals(configClass);
    }
}
