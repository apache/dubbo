package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.util.ConfigValidationUtils;
import org.apache.dubbo.rpc.InvokerListener;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

@Activate
public class ReferenceConfigValidator implements ConfigValidator<ReferenceConfig<?>> {

    @Override
    public void validate(ReferenceConfig<?> config) {
        validateReferenceConfig(config);
    }

    public static void validateReferenceConfig(ReferenceConfig<?> config) {
        ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), InvokerListener.class, "listener", config.getListener());
        ConfigValidationUtils.checkKey(VERSION_KEY, config.getVersion());
        ConfigValidationUtils.checkKey(GROUP_KEY, config.getGroup());
        ConfigValidationUtils.checkName(CLIENT_KEY, config.getClient());

        InterfaceConfigValidator.validateAbstractInterfaceConfig(config);

        List<RegistryConfig> registries = config.getRegistries();
        if (registries != null) {
            for (RegistryConfig registry : registries) {
                registry.validate();
            }
        }

        ConsumerConfig consumerConfig = config.getConsumer();
        if (consumerConfig != null) {
            consumerConfig.validate();
        }
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ReferenceConfig.class.equals(configClass);
    }
}