package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.context.ConfigValidator;

@Activate
public class SslConfigValidator implements ConfigValidator<SslConfig> {

    public static void validateSslConfig(SslConfig sslConfig) {
        // TODO
    }

    @Override
    public void validate(SslConfig config) {
        validateSslConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return SslConfig.class.equals(configClass);
    }
}
