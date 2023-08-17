package org.apache.dubbo.config.utils.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;

import static org.apache.dubbo.common.constants.CommonConstants.FILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PASSWORD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.USERNAME_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.remoting.Constants.TRANSPORTER_KEY;

@Activate
public class RegistryConfigValidator implements ConfigValidator<RegistryConfig> {

    @Override
    public void validate(RegistryConfig config) {
        validateRegistryConfig(config);
    }

    public static void validateRegistryConfig(RegistryConfig config) {
        ConfigValidationUtils.checkName(PROTOCOL_KEY, config.getProtocol());
        ConfigValidationUtils.checkName(USERNAME_KEY, config.getUsername());
        ConfigValidationUtils.checkLength(PASSWORD_KEY, config.getPassword());
        ConfigValidationUtils.checkPathLength(FILE_KEY, config.getFile());
        ConfigValidationUtils.checkName(TRANSPORTER_KEY, config.getTransporter());
        ConfigValidationUtils.checkName(SERVER_KEY, config.getServer());
        ConfigValidationUtils.checkName(CLIENT_KEY, config.getClient());
        ConfigValidationUtils.checkParameterName(config.getParameters());
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return RegistryConfig.class.isAssignableFrom(configClass);
    }
}
